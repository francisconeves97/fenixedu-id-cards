package org.fenixedu.idcards.controller;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.security.SkipCSRF;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.idcards.domain.RaspberryPiSession;
import org.fenixedu.idcards.domain.SantanderCardInfo;
import org.fenixedu.idcards.domain.SantanderCardState;
import org.fenixedu.idcards.domain.SantanderEntry;
import org.fenixedu.idcards.dto.DeliverSessionMifareRequest;
import org.fenixedu.idcards.service.SantanderIdCardsService;
import org.fenixedu.santandersdk.exception.SantanderValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import pt.ist.fenixframework.FenixFramework;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/idcards")
public class IdCardsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdCardsController.class);

    private SantanderIdCardsService cardService;

    @Autowired
    public IdCardsController(SantanderIdCardsService cardService) {
        this.cardService = cardService;
    }

    @RequestMapping(value = "{username}", method = RequestMethod.GET)
    public ResponseEntity<?> getUserCards(@PathVariable String username, User user) {
        User usernameUser = User.findByUsername(username);

        if (usernameUser == null || (!isIdCardManager(user) && user != usernameUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return getUserCardsResponse(usernameUser);
    }

    private ResponseEntity<?> getUserCardsResponse(final User user) {
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        cardService.getOrUpdateState(user);
        return ResponseEntity.ok(cardService.getUserSantanderCards(user));
    }

    @RequestMapping(value = "user-info", method = RequestMethod.GET)
    public ResponseEntity<?> userInfo(User user) {
        JsonObject response = new JsonObject();
        response.addProperty("admin", isIdCardManager(user));
        response.addProperty("canRequestCard", cardService.canRequestCard(user));
        response.addProperty("language", I18N.getLocale().getLanguage());
        return ResponseEntity.ok(response.toString());
    }

    @SkipCSRF
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> requestCard(@RequestHeader("X-Requested-With") String requestedWith, User user,
            @RequestBody(required = false) String requestReason) {

        if (!cardService.canRequestCard(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            SantanderEntry entry = cardService.createRegister(user, requestReason);
            cardService.sendRegister(user, entry);
        } catch (SantanderValidationException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(cardService.getErrorMessage(user.getProfile().getPreferredLocale(), e.getMessage()));
        }

        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "preview", method = RequestMethod.GET)
    public ResponseEntity<?> previewCard(User user) {
        try {
            return ResponseEntity.ok(cardService.generateCardPreview(user));
        } catch (SantanderValidationException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", cardService.getErrorMessage(user.getProfile().getPreferredLocale(), e.getMessage()));
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(error.toString());
        }
    }

    @RequestMapping(value = "deliver/session-mifare", method = RequestMethod.GET)
    public ResponseEntity<?> getSessionMifare(User user) {
        if (isIdCardManager(user)) {
            RaspberryPiSession raspberryPiSession = user.getRaspberryPiSession();

            if (raspberryPiSession == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            } else if (raspberryPiSession.getUserMifare() == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(raspberryPiSession);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @RequestMapping(value = "deliver/session-mifare", method = RequestMethod.PUT)
    public ResponseEntity<?> deliverSessionMifare(@RequestBody DeliverSessionMifareRequest request, User user) {
        if (isIdCardManager(user)) {
            User userToDeliver = User.findByUsername(request.getIstId());

            if (userToDeliver == null) {
                return ResponseEntity.notFound().build();
            }

            FenixFramework.atomic(() -> {
                SantanderCardInfo card = userToDeliver.getCurrentSantanderEntry().getSantanderCardInfo();

                card.setMifareNumber(request.getMifare());
                card.getSantanderEntry().updateState(SantanderCardState.DELIVERED);
            });

            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @SkipCSRF
    @RequestMapping(value = "deliver/{mifare}", method = RequestMethod.PUT)
    public ResponseEntity<?> deliver(@PathVariable String mifare, @RequestHeader("X-Requested-With") String requestedWith, HttpServletRequest request) {
        try {
            final Long mifareNumber = Long.parseLong(mifare);
            return Bennu.getInstance().getSantanderCardInfoSet().stream()
                    .filter(card -> !Strings.isNullOrEmpty(card.getMifareNumber())).filter(card -> {
                        try {
                            final Long cardMifareNumber = Long.parseLong(card.getMifareNumber());
                            return cardMifareNumber.equals(mifareNumber);
                        } catch (NumberFormatException nfe) {
                            LOGGER.error("Error parsing >{}< for card {}", card.getMifareNumber(), card.getExternalId());
                            return false;
                        }
                    }).findAny().map(card -> {
                        FenixFramework.atomic(() -> {
                            if (card.getSantanderEntry().getState().equals(SantanderCardState.DELIVERED) && isIdCardManager(card.getSantanderEntry().getUser())) {
                                RaspberryPiSession.init(request.getRemoteAddr(), card.getSantanderEntry().getUser());
                            } else {
                                registerMifareSession(request.getRemoteAddr(), mifare);
                                card.getSantanderEntry().updateState(SantanderCardState.DELIVERED);
                            }
                        });
                        return ResponseEntity.ok().build();
                    }).orElseGet(() -> {
                        registerMifareSession(request.getRemoteAddr(), mifare);
                        return ResponseEntity.notFound().build();
                    });
        } catch (NumberFormatException nfe) {
            LOGGER.error("Error parsing number", nfe);
            JsonObject error = new JsonObject();
            error.addProperty("error", nfe.getMessage());
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(error.toString());
        }
    }

    private void registerMifareSession(String ipAddress, String mifare) {
        FenixFramework.atomic(() -> {
            RaspberryPiSession raspberryPiSession = RaspberryPiSession.getSessionByIpAddress(ipAddress);
            if (raspberryPiSession != null) {
                raspberryPiSession.setUserMifare(mifare);
            }
        });
    }

    @SkipCSRF
    @RequestMapping(value = "/{card}/deliver", method = RequestMethod.PUT)
    public ResponseEntity<?> deliverCard(@PathVariable SantanderCardInfo card, User user,
            @RequestHeader("X-Requested-With") String requestedWith) {
        if (isIdCardManager(user)) {
            FenixFramework.atomic(() -> {
                card.getSantanderEntry().updateState(SantanderCardState.DELIVERED);
            });
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private boolean isIdCardManager(User user) {
        return Group.dynamic("idCardManager").isMember(user);
    }
}
