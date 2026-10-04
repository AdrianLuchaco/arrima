package es.arrima.prize;

import es.arrima.files.FileStorage;
import es.arrima.files.ImageType;
import es.arrima.files.ImageValidator;
import es.arrima.files.StorageCleanup;
import es.arrima.files.StoredFilePaths;
import es.arrima.international.InternationalService;
import es.arrima.international.PrizeWinner;
import es.arrima.melee.Melee;
import es.arrima.melee.MeleeAccess;
import es.arrima.melee.MeleeDeletionEvent;
import es.arrima.melee.MeleeStatus;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrizeService {

    /** Limits that keep the free 1 GB of storage from filling up (a compressed photo is ~300 KB). */
    public static final int MAX_PHOTOS_PER_PRIZE = 10;
    public static final int MAX_PHOTOS_PER_MELEE = 60;

    private final PrizeRepository prizeRepository;
    private final PrizePhotoRepository photoRepository;
    private final InternationalService internationalService;
    private final MeleeAccess meleeAccess;
    private final ImageValidator imageValidator;
    private final FileStorage fileStorage;
    private final StorageCleanup storageCleanup;
    private final Clock clock;

    public PrizeService(PrizeRepository prizeRepository, PrizePhotoRepository photoRepository,
            InternationalService internationalService, MeleeAccess meleeAccess, ImageValidator imageValidator,
            FileStorage fileStorage, StorageCleanup storageCleanup, Clock clock) {
        this.prizeRepository = prizeRepository;
        this.photoRepository = photoRepository;
        this.internationalService = internationalService;
        this.meleeAccess = meleeAccess;
        this.imageValidator = imageValidator;
        this.fileStorage = fileStorage;
        this.storageCleanup = storageCleanup;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Prize> prizesOf(long meleeId) {
        return prizeRepository.findByMeleeIdOrderByPosition(meleeId);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<PrizePhoto>> photosOf(List<Prize> prizes) {
        if (prizes.isEmpty()) {
            return Map.of();
        }
        return photoRepository.findByPrizeIdInOrderById(prizes.stream().map(Prize::getId).toList()).stream()
                .collect(Collectors.groupingBy(PrizePhoto::getPrizeId));
    }

    @Transactional(readOnly = true)
    public long photoCount(long meleeId) {
        return photoRepository.countByMeleeId(meleeId);
    }

    /** Photos of teams that would no longer have a prize if the ceremony started (again) now. */
    @Transactional(readOnly = true)
    public long photosLostIfStarted(Melee melee) {
        Set<Long> winners = internationalService.prizeWinners(melee).stream().map(PrizeWinner::teamId).collect(Collectors.toSet());
        List<Prize> losing = prizesOf(melee.getId()).stream().filter(prize -> !winners.contains(prize.getTeamId())).toList();
        return photosOf(losing).values().stream().mapToLong(List::size).sum();
    }

    /**
     * "Entrega de premios". If it had started before (going back to correct la Internacional), a
     * team that still has a prize keeps its photos; a team that lost it loses them.
     */
    @Transactional
    public void start(Melee melee) {
        melee.requireStatus(MeleeStatus.INTERNATIONAL);
        List<PrizeWinner> winners = internationalService.prizeWinners(melee);
        Map<Long, Prize> existing = prizesOf(melee.getId()).stream()
                .collect(Collectors.toMap(Prize::getTeamId, Function.identity()));
        for (PrizeWinner winner : winners) {
            Prize prize = existing.remove(winner.teamId());
            if (prize != null) {
                prize.moveTo(winner.position(), winner.points());
            } else {
                prizeRepository.save(new Prize(melee.getId(), winner.position(), winner.teamId(), winner.points()));
            }
        }
        deletePrizes(new ArrayList<>(existing.values()));
        melee.moveTo(MeleeStatus.PRIZES, clock.instant());
        meleeAccess.recordChange(melee);
    }

    /** The ceremony goes from the last prize to the first; spectators see each one as it is handed out. */
    @Transactional
    public void markAwarded(long meleeId, long clubId, long prizeId) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.PRIZES);
        findInMelee(melee, prizeId).markAwarded(clock.instant());
        meleeAccess.recordChange(melee);
    }

    @Transactional
    public void addPhoto(long meleeId, long clubId, long prizeId, byte[] content) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.PRIZES);
        Prize prize = findInMelee(melee, prizeId);
        if (photoRepository.countByPrizeId(prizeId) >= MAX_PHOTOS_PER_PRIZE
                || photoRepository.countByMeleeId(melee.getId()) >= MAX_PHOTOS_PER_MELEE) {
            throw new ApiException(ErrorCode.PHOTO_LIMIT,
                    Map.of("maxPerPrize", MAX_PHOTOS_PER_PRIZE, "maxPerMelee", MAX_PHOTOS_PER_MELEE));
        }
        ImageType type = imageValidator.validate(content);
        String path = StoredFilePaths.prizePhoto(melee.getClubId(), melee.getId(), type);
        fileStorage.store(path, content, type.contentType());
        photoRepository.save(new PrizePhoto(prize.getId(), path, type.contentType(), content.length, clock.instant()));
        meleeAccess.recordChange(melee);
    }

    @Transactional
    public void deletePhoto(long meleeId, long clubId, long prizeId, long photoId) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.PRIZES);
        Prize prize = findInMelee(melee, prizeId);
        PrizePhoto photo = photoRepository.findByIdAndPrizeId(photoId, prize.getId()).orElseThrow(ApiException::notFound);
        photoRepository.delete(photo);
        storageCleanup.deleteAfterCommit(List.of(photo.getStoragePath()));
        meleeAccess.recordChange(melee);
    }

    /** The photo of the prize that goes to the WhatsApp group; until one is chosen, the first. */
    @Transactional
    public void chooseMainPhoto(long meleeId, long clubId, long prizeId, long photoId) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.PRIZES);
        Prize prize = findInMelee(melee, prizeId);
        photoRepository.findByIdAndPrizeId(photoId, prize.getId()).orElseThrow(ApiException::notFound);
        photoRepository.clearMain(prize.getId());
        photoRepository.markMain(photoId);
        meleeAccess.recordChange(melee);
    }

    /**
     * The admin confirmed that the prizes reached the club's group: the app cannot know it by
     * itself. They can be sent and confirmed again, until the melee closes.
     */
    @Transactional
    public void markShared(long meleeId, long clubId) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.markPrizesShared(clock.instant());
        meleeAccess.recordChange(melee);
    }

    /** Used when teams, the schedule or la Internacional are redone. */
    @Transactional
    public void deleteAll(Melee melee) {
        deletePrizes(prizesOf(melee.getId()));
    }

    /**
     * Rows of a deleted melee go with the database cascade; its photos live in Storage and are
     * removed once the deletion commits.
     */
    @EventListener
    public void onMeleeDeletion(MeleeDeletionEvent event) {
        List<Prize> prizes = prizesOf(event.meleeId());
        storageCleanup.deleteAfterCommit(photosOf(prizes).values().stream()
                .flatMap(List::stream).map(PrizePhoto::getStoragePath).toList());
    }

    private void deletePrizes(List<Prize> prizes) {
        List<PrizePhoto> photos = photosOf(prizes).values().stream().flatMap(List::stream).toList();
        photoRepository.deleteAll(photos);
        prizeRepository.deleteAll(prizes);
        prizeRepository.flush();
        storageCleanup.deleteAfterCommit(photos.stream().map(PrizePhoto::getStoragePath).toList());
    }

    private Prize findInMelee(Melee melee, long prizeId) {
        return prizeRepository.findByIdAndMeleeId(prizeId, melee.getId()).orElseThrow(ApiException::notFound);
    }
}
