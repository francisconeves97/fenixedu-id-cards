package org.fenixedu.idcards.tasks;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.spring.BennuSpringContextHelper;
import org.fenixedu.idcards.exception.SantanderCardNoPermissionException;
import org.fenixedu.idcards.notifications.CardNotifications;
import org.fenixedu.idcards.service.SantanderIdCardsService;
import org.fenixedu.santandersdk.dto.RegisterAction;
import org.fenixedu.santandersdk.exception.SantanderMissingInformationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ist.fenixframework.Atomic;

import java.util.List;
import java.util.Set;

public class SantanderCardsRequestTask extends CronTask {

    private static final Logger logger = LoggerFactory.getLogger(SantanderCardsRequestTask.class);
    private SantanderIdCardsService cardsService = BennuSpringContextHelper.getBean(SantanderIdCardsService.class);

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.WRITE;
    }

    @Override
    public void runTask() throws Exception {
        SantanderIdCardsService idCardsService = BennuSpringContextHelper.getBean(SantanderIdCardsService.class);
        Set<User> users = Bennu.getInstance().getUserSet();

        for (User user : users) {
            List<RegisterAction> userActions = idCardsService.getPersonAvailableActions(user);
            try {
                RegisterAction registerAction = null;

                if (userActions.contains(RegisterAction.NOVO)) {
                    registerAction = RegisterAction.NOVO;
                } else if (userActions.contains(RegisterAction.RENU)) {
                    registerAction = RegisterAction.RENU;
                }

                if (registerAction != null) {
                    idCardsService.createRegister(user, registerAction, "automatic card generation");
                    taskLog("Requested card for user %s%n", user.getUsername());
                } else {
                    taskLog("No available register action for user: %s", user.getUsername());
                }

                Thread.sleep(5000);
            } catch (SantanderCardNoPermissionException e) {
                taskLog("No permission to request card for user %s%n", user.getUsername());
            } catch (SantanderMissingInformationException se) {
                taskLog("User %s cant request a card because: %s%n", user.getUsername(), se.getMessage());
                CardNotifications.notifyMissingInformation(user,
                        cardsService.getErrorMessage(user.getProfile().getPreferredLocale(), se.getMessage()));
            } catch (RuntimeException e) {
                taskLog("Couldn't request card for user %s%n", user.getUsername());
                Thread.sleep(5000);
            }
        }
    }
}
