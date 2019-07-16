package org.fenixedu.idcards.tasks;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.idcards.domain.SantanderCardState;
import org.fenixedu.idcards.domain.SantanderEntry;
import org.fenixedu.idcards.notifications.CardNotifications;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ist.fenixframework.Atomic;

@Task(englishTitle = "Notify users with expiring cards", readOnly = true)
public class CardsRemindersTask extends CronTask {

    private static final Logger logger = LoggerFactory.getLogger(CardsRemindersTask.class);
    private final int DAYS_TO_EXPIRE = 30;

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.WRITE;
    }

    @Override
    public void runTask() {
        Bennu.getInstance().getUserSet().stream().filter(u -> u.getCurrentSantanderEntry() != null &&
                SantanderCardState.ISSUED.equals(u.getCurrentSantanderEntry().getState()))
                .forEach(this::remindUser);
    }

    private void remindUser(User user) {

        SantanderEntry entry = user.getCurrentSantanderEntry();
        SantanderCardState newState = entry.getState();
        if (SantanderCardState.DELIVERED.equals(newState) && !entry.getWasExpiringNotified() && DateTime.now().isAfter(entry.getSantanderCardInfo()
                .getExpiryDate().minusDays(DAYS_TO_EXPIRE))) {
            entry.setWasExpiringNotified(true);
            CardNotifications.notifyCardExpiring(user);
            taskLog("Notifying user for expiring card: %s%n", user.getUsername());
        } else if (SantanderCardState.ISSUED.equals(entry.getState()) && !entry.getWasPickupNotified() && DateTime.now().isAfter(entry.getSantanderCardInfo()
                .getLastTransition().getTransitionDate().plusDays(15))) {
            entry.setWasPickupNotified(true);
            CardNotifications.notifyCardPickup(user);
            taskLog("Notifying user to pickup card: %s%n", user.getUsername());
        }
    }
}
