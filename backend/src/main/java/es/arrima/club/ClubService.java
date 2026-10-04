package es.arrima.club;

import es.arrima.files.FileLinkSigner;
import es.arrima.files.FileStorage;
import es.arrima.files.ImageType;
import es.arrima.files.ImageValidator;
import es.arrima.files.StoredFilePaths;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.text.TextInput;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClubService {

    public static final int MAX_NAME_LENGTH = 100;

    private static final Logger log = LoggerFactory.getLogger(ClubService.class);

    private final ClubRepository clubRepository;
    private final FileStorage fileStorage;
    private final ImageValidator imageValidator;
    private final FileLinkSigner fileLinkSigner;
    private final Clock clock;

    public ClubService(ClubRepository clubRepository, FileStorage fileStorage, ImageValidator imageValidator,
            FileLinkSigner fileLinkSigner, Clock clock) {
        this.clubRepository = clubRepository;
        this.fileStorage = fileStorage;
        this.imageValidator = imageValidator;
        this.fileLinkSigner = fileLinkSigner;
        this.clock = clock;
    }

    /** New clubs start with the default settings and points; the admin adjusts them in the profile. */
    @Transactional
    public Club createClub(String rawName) {
        String name = TextInput.require(rawName, "clubName", MAX_NAME_LENGTH);
        return clubRepository.save(new Club(name, clock.instant()));
    }

    @Transactional(readOnly = true)
    public Club getClub(long clubId) {
        return clubRepository.findById(clubId).orElseThrow(ApiException::notFound);
    }

    @Transactional(readOnly = true)
    public ClubProfileResponse getProfile(long clubId) {
        return toResponse(getClub(clubId));
    }

    @Transactional
    public ClubProfileResponse updateProfile(long clubId, UpdateClubProfileRequest request) {
        Club club = getClub(clubId);
        String name = TextInput.require(request.name(), "name", MAX_NAME_LENGTH);
        MeleeSettings defaults = new MeleeSettings(request.courtCount(), request.roundsCount(), request.prizeCount(),
                request.entryFeeCents());
        club.updateProfile(name, defaults, request.scoring().toScoringTable(), clock.instant());
        return toResponse(club);
    }

    @Transactional
    public ClubProfileResponse changeLogo(long clubId, byte[] content) {
        Club club = getClub(clubId);
        ImageType type = imageValidator.validate(content);
        String newPath = StoredFilePaths.clubLogo(clubId, type);
        fileStorage.store(newPath, content, type.contentType());
        String previousPath = club.getLogoPath();
        club.changeLogo(newPath, clock.instant());
        deleteQuietly(previousPath);
        return toResponse(club);
    }

    @Transactional
    public ClubProfileResponse removeLogo(long clubId) {
        Club club = getClub(clubId);
        String previousPath = club.getLogoPath();
        club.changeLogo(null, clock.instant());
        deleteQuietly(previousPath);
        return toResponse(club);
    }

    /** A leftover file only wastes a little storage, so failing to delete it must not fail the request. */
    private void deleteQuietly(String path) {
        if (path == null) {
            return;
        }
        try {
            fileStorage.delete(path);
        } catch (RuntimeException e) {
            log.warn("Could not delete stored file {}", path, e);
        }
    }

    private ClubProfileResponse toResponse(Club club) {
        MeleeSettings defaults = club.getMeleeDefaults();
        return new ClubProfileResponse(club.getName(), fileLinkSigner.link(club.getLogoPath()),
                defaults.courtCount(), defaults.roundsCount(), defaults.prizeCount(), defaults.entryFeeCents(),
                ScoringTableDto.from(club.getScoring()));
    }
}
