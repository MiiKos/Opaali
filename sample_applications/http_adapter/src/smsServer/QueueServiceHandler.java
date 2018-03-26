/*
 * Opaali (Telia Operator Service Platform) sample code
 *
 * Copyright(C) 2018 Telia Company
 *
 * Telia Operator Service Platform and Telia Opaali Portal are trademarks of Telia Company.
 *
 * Author: jlasanen
 *
 */


package smsServer;

import OpaaliAPI.Config;

/*
 * a wrapping around QueueService that adds support fir service configuration
 */
public class QueueServiceHandler extends QueueService {

    QueueServiceHandler(ServiceConfig sc) {

        sc.setValidity(true);   // default
        this.sc = sc;
        this.serviceName = sc.getServiceName();

        qSize = sc.getConfigEntryInt(ServerConfig.CONFIG_QSIZE);
        if (qSize <= 0) {
            qSize = DEFAULT_QSIZE;
        }

        StrMask[] logMasks = StrMask.parseMaskConfig(sc.getConfigEntry(ServerConfig.CONFIG_LOG_MASK));
        if (logMasks.length == 0) {
            logMasks = null;
        }

        isValid = (qSvc == null);    // valid only for a single queue service

        if (isValid) {
            Config.setServiceConfig(serviceName, sc);
            //qSvc = QueueService.create(qSize);
            qSvc = create(qSize);
            qSvc.logRequest = new RequestLogger(logMasks);;
        }
        else {
            sc.setValidity(false);
            failureMode = true;
        }

    }

    String serviceName = null;
    ServiceConfig sc = null;
    int qSize = DEFAULT_QSIZE;

    boolean failureMode = false;         // true after an unrecoverable error
    private boolean isValid = true;
    private static QueueService qSvc = null;


    /*
     * this is to be called by queue users to get a handle to the queue
     */
    public static QueueService create() {
        if (qSvc == null) {
            qSvc = new QueueService(DEFAULT_QSIZE);
        }
        return qSvc;
    }

    public static QueueService get() {
        if (qSvc == null) {
            qSvc = new QueueService(DEFAULT_QSIZE);
        }
        return qSvc;
    }


    private QueueServiceHandler() {}
}
