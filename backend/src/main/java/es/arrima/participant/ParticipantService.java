package es.arrima.participant;

import es.arrima.melee.Melee;
import es.arrima.melee.MeleeAccess;
import es.arrima.melee.MeleeStatus;
import es.arrima.participant.whatsapp.ParsedEntry;
import es.arrima.participant.whatsapp.WhatsAppListParser;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import es.arrima.shared.text.TextInput;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipantService {

    public static final int MAX_PER_MELEE = 300;
    public static final int MAX_LIST_NUMBER = 999;

    /** Late arrivals can still sign up after the draw, to replace someone who left. */
    private static final MeleeStatus[] SIGN_UP_PHASES = {MeleeStatus.REGISTRATION, MeleeStatus.TEAMS, MeleeStatus.MATCHES};

    private final ParticipantRepository participantRepository;
    private final MeleeAccess meleeAccess;
    private final WhatsAppListParser whatsAppListParser = new WhatsAppListParser();
    private final Clock clock;

    public ParticipantService(ParticipantRepository participantRepository, MeleeAccess meleeAccess, Clock clock) {
        this.participantRepository = participantRepository;
        this.meleeAccess = meleeAccess;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Participant> listForMelee(long meleeId) {
        return participantRepository.findByMeleeIdInListOrder(meleeId);
    }

    /** Reads a pasted WhatsApp list without saving anything, flagging likely duplicates. */
    @Transactional(readOnly = true)
    public List<ImportPreviewEntry> previewImport(long meleeId, long clubId, String text) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        Set<String> registeredNames = participantRepository.findByMeleeIdInListOrder(melee.getId()).stream()
                .map(participant -> NameKey.of(participant.getDisplayName()))
                .collect(Collectors.toSet());
        return whatsAppListParser.parse(text).stream()
                .map(entry -> toPreview(entry, registeredNames.contains(NameKey.of(entry.name()))))
                .toList();
    }

    /** Saves the people the admin confirmed in the preview, keeping their list numbers. */
    @Transactional
    public void importParticipants(long meleeId, long clubId, List<NewParticipant> people) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(SIGN_UP_PHASES);
        requireRoomFor(melee, people.size());
        Instant now = clock.instant();
        for (int i = 0; i < people.size(); i++) {
            NewParticipant person = people.get(i);
            String field = "participants[" + i + "]";
            participantRepository.save(new Participant(melee.getId(), checkedNumber(person.listNumber(), field + ".listNumber"),
                    TextInput.require(person.name(), field + ".name", WhatsAppListParser.MAX_NAME_LENGTH), now));
        }
        meleeAccess.recordChange(melee);
    }

    @Transactional
    public Participant add(long meleeId, long clubId, NewParticipant person) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(SIGN_UP_PHASES);
        requireRoomFor(melee, 1);
        Participant participant = participantRepository.save(new Participant(melee.getId(),
                checkedNumber(person.listNumber(), "listNumber"), checkedName(person.name()), clock.instant()));
        meleeAccess.recordChange(melee);
        return participant;
    }

    /** Fixing a name or number is allowed until the melee is closed. */
    @Transactional
    public void edit(long meleeId, long clubId, long participantId, NewParticipant person) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.REGISTRATION, MeleeStatus.TEAMS, MeleeStatus.MATCHES,
                MeleeStatus.INTERNATIONAL, MeleeStatus.PRIZES);
        Participant participant = findInMelee(melee, participantId);
        participant.edit(checkedNumber(person.listNumber(), "listNumber"), checkedName(person.name()));
        meleeAccess.recordChange(melee);
    }

    /** "Anular": signed up but not here. Kept on the list, never drawn. */
    @Transactional
    public void withdraw(long meleeId, long clubId, long participantId) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(SIGN_UP_PHASES);
        findInMelee(melee, participantId).withdraw();
        meleeAccess.recordChange(melee);
    }

    @Transactional
    public void reinstate(long meleeId, long clubId, long participantId) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(SIGN_UP_PHASES);
        findInMelee(melee, participantId).reinstate();
        meleeAccess.recordChange(melee);
    }

    /** Removing someone completely (typed by mistake) is only possible before the draw. */
    @Transactional
    public void delete(long meleeId, long clubId, long participantId) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.REGISTRATION);
        participantRepository.delete(findInMelee(melee, participantId));
        meleeAccess.recordChange(melee);
    }

    public Participant findInMelee(Melee melee, long participantId) {
        return participantRepository.findByIdAndMeleeId(participantId, melee.getId()).orElseThrow(ApiException::notFound);
    }

    private void requireRoomFor(Melee melee, int newPeople) {
        if (participantRepository.countByMeleeId(melee.getId()) + newPeople > MAX_PER_MELEE) {
            throw new ApiException(ErrorCode.PARTICIPANT_LIMIT, Map.of("max", MAX_PER_MELEE));
        }
    }

    private static String checkedName(String name) {
        return TextInput.require(name, "name", WhatsAppListParser.MAX_NAME_LENGTH);
    }

    private static Integer checkedNumber(Integer listNumber, String field) {
        if (listNumber != null && (listNumber < 1 || listNumber > MAX_LIST_NUMBER)) {
            throw ApiException.validation(field, "Range");
        }
        return listNumber;
    }

    private static ImportPreviewEntry toPreview(ParsedEntry entry, boolean alreadyRegistered) {
        return new ImportPreviewEntry(entry.listNumber(), entry.name(), entry.struckThrough(), entry.beforeListStart(),
                alreadyRegistered);
    }
}
