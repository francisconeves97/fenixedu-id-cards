package org.fenixedu.idcards.domain;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.joda.time.DateTime;

public class RaspberryPiSession extends RaspberryPiSession_Base {

    private RaspberryPiSession(String ipAddress) {
        super();
        setIpAddress(ipAddress);
    }

    public static RaspberryPiSession init(String ipAddress, User manager) {
        RaspberryPiSession piSession =  Bennu.getInstance().getRaspberryPiSessionSet().stream()
                .filter(r -> r.getIpAddress().equals(ipAddress))
                .findAny()
                .orElseGet(() -> new RaspberryPiSession(ipAddress));

        piSession.setManager(manager);
        piSession.setCreatedAt(new DateTime());
        piSession.setUserMifare(null);
        return piSession;
    }

    public static RaspberryPiSession getSessionByIpAddress(String ipAddress) {
        return Bennu.getInstance().getRaspberryPiSessionSet().stream()
                .filter(r -> r.getIpAddress().equals(ipAddress))
                .findAny()
                .orElse(null);
    }
}
