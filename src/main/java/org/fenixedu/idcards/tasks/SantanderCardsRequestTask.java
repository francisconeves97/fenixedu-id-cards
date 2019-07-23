package org.fenixedu.idcards.tasks;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.spring.BennuSpringContextHelper;
import org.fenixedu.idcards.domain.SantanderEntry;
import org.fenixedu.idcards.exception.SantanderCardNoPermissionException;
import org.fenixedu.idcards.notifications.CardNotifications;
import org.fenixedu.idcards.service.SantanderIdCardsService;
import org.fenixedu.santandersdk.dto.RegisterAction;
import org.fenixedu.santandersdk.exception.SantanderMissingInformationException;
import org.fenixedu.santandersdk.exception.SantanderValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

@Task(englishTitle = "Requests users first IST cards", readOnly = true)
public class SantanderCardsRequestTask extends CronTask {

    private static final Logger logger = LoggerFactory.getLogger(SantanderCardsRequestTask.class);

    private SantanderIdCardsService idCardsService = BennuSpringContextHelper.getBean(SantanderIdCardsService.class);

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    public void runTask() {
        Bennu.getInstance().getUserSet().stream().filter(u -> u.getCurrentSantanderEntry() == null) //TODO remove limit
                .forEach(this::requestCard);
    }

    public void requestCard(User user) {
        FenixFramework.atomic(() -> {
            try {
                SantanderEntry createRegister =
                        idCardsService.createRegister(user, RegisterAction.NOVO, "First card automatic request");
                idCardsService.sendRegister(user, createRegister);
                taskLog("Created card with success for user %s%n", user.getUsername()); //TODO remove this
            } catch (SantanderCardNoPermissionException scnpe) {
                taskLog("No permission to request card for user %s%n", user.getUsername());
                return;
            } catch (SantanderMissingInformationException smie) {
                taskLog("User %s has missing information: %s%n", user.getUsername(), smie.getMessage());
                notifyMissingInformation(user, smie.getMessage());
                return;
            } catch (SantanderValidationException sve) {
                taskLog("Error generating card for %s (current SantanderEntry: %s): %s%n", user.getUsername(),
                        user.getCurrentSantanderEntry().getExternalId(), sve.getMessage());
                return;
            } catch (Exception e) {
                taskLog("Failed for user %s (current SantanderEntry: %s)%n", user.getUsername(),
                        user.getCurrentSantanderEntry().getExternalId());
            }
        });
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
    }

    private void notifyMissingInformation(User user, String errors) {
        Locale locale = user.getProfile().getPreferredLocale();
        CardNotifications.notifyMissingInformation(user, idCardsService.getErrorMessage(locale, errors));
    }
}