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

import java.util.concurrent.ArrayBlockingQueue;
import CgwCompatibility.CgwHttpRequest;
import CgwCompatibility.CgwMessage;
import CgwCompatibility.KeywordMapper;
import OpaaliAPI.HttpResponse;
import OpaaliAPI.Log;


/*
 * internal queue for performing http requests asynchronously
 *
 * - multiple producers may submit items to the queue, ordering is not guaranteed
 * - a single thread will consume the queue to avoid deadlocks
 *
 */
public class QueueService {

    /*
     * data item to be queued
     */
    private class QItem {

        public QItem(String id, String url, String replyUrl) {
            this.id = (id != null ? id : new Long(++msgCounter).toString());
            this.url = url;
            this.replyUrl = replyUrl;
        }

        public QItem(String url, String replyUrl) {
            this(null, url, replyUrl);
        }

        String id = new Long(++msgCounter).toString();    // message id
        String url = null;                                // http get request URL
        String replyUrl = null;                           // http get request URL for replies (optional)
    }

    // counter for generating (not globally unique) message ids
    private static long msgCounter = 0;

    protected static final int DEFAULT_QSIZE = 20;
    private static QueueService qSvc = null;

    /*
     * initialize QueueService
     * - queue size provided
     */
    public static QueueService create(int qsize) {
        if (qSvc == null) {
            qSvc = new QueueService(qsize);
        }
        return qSvc;
    }

    /*
     * initialize QueueService
     * - default queue size used
     */
    public static QueueService create() {
        if (qSvc == null) {
            qSvc = new QueueService(DEFAULT_QSIZE);
        }
        return qSvc;
    }


    /*
     * a blocking service loop
     */
    public void loop() {
        serviceLoop();
    }

    public void shutdown() {
        Log.logInfo("shutting down internal queue service...");
        shutdown = true;
    }

    /*
     * submitting a http GET request to queue, specifying
     * - url
     * - replyUrl (optional)
     */
    synchronized public boolean submit(String reserved, String url, String replyUrl) {
        /*
         * submitters should never take anything from the queue to avoid problems
         * if overflow is not null the queue is full (and only this call should
         * set it to non-null value
         */
        if (overflow == null) {
            QItem qi = new QItem(url, replyUrl);
            if (!q.offer(qi)) {
                overflow = qi;
            }
            return true;
        }
        return false;
    }


    /*
     * submitting a http GET request to queue, specifying
     * - message id for log messages
     * - url
     * - replyUrl (optional)
     */
    synchronized public boolean submit(String reserved, String id, String url, String replyUrl) {
        /*
         * submitters should never take anything from the queue to avoid problems
         * if overflow is not null the queue is full (and only this call should
         * set it to non-null value
         */
        if (overflow == null) {
            QItem qi = new QItem(id, url, replyUrl);
            if (!q.offer(qi)) {
                overflow = qi;
            }
            return true;
        }
        return false;
    }




    // = end of public part ===============================================

    protected QueueService() {}

    protected QueueService(int qSize) {
        this.qSize = qSize;
        q = new ArrayBlockingQueue<QItem>(qSize);
    }

    private int qSize = DEFAULT_QSIZE;

    private ArrayBlockingQueue<QItem> q = null;
    private QItem overflow = null;

    private boolean shutdown = false;
    private String qname = null;

    protected RequestLogger logRequest = new RequestLogger(null);

    private void serviceLoop() {
        //QItem qi = q.take();
        while (!shutdown) {
            QItem qi = consume(qname);
            if (qi != null && qi.url != null) {
                // send by making a http GET-request to the target url

                long startTime = RequestLogger.getTimeNow();

                HttpResponse resp = CgwHttpRequest.get(qi.url);

                long endTime = RequestLogger.getTimeNow();

                if (resp != null) {

                    logRequest.log("queued "+qi.url, resp.rc, -1, endTime-startTime);
                    //Log.logInfo("Queued request made for message "+qi.id+" (response in "+(endTime-startTime)+"ms: "+result: "+resp.rc+" "+(endTime-startTime)+"ms), qsize="+q.size());"
                    //        + "logRequest.

                    if (qi.replyUrl != null && resp.responseBody != null && resp.responseBody.length() > 0) {
                        /*
                         * if a successful response contained a body, return that content
                         * by generating a separate MT send request
                         */
                        /*
                         * expand replyUrl second time to fill in the message,
                         * this relies on the replyUrl containing the macro $(MSG)
                         * which relies the original replyUrl having contained $$(MSG)
                         * ( $$ is interpreted as escaped $ )
                         */
                        String replyUrl = KeywordMapper.tmplExpand(qi.replyUrl, new CgwMessage(null,null,null,null,resp.responseBody));

                        startTime = RequestLogger.getTimeNow();

                        resp = CgwHttpRequest.get(replyUrl);

                        endTime = RequestLogger.getTimeNow();

                        if (resp != null) {
                            logRequest.log("queued reply "+replyUrl, resp.rc, -1, endTime-startTime);

                            //Log.logInfo("Queued reply request made for message "+qi.id+" ("+(endTime-startTime)+"ms), qsize="+q.size());
                        }
                    }
                }
                else {
                    // failed to make a http request
                    logRequest.log("Failed to make a queued request "+qi.url, -1, -1, endTime-startTime);
                    //Log.logError("Failed to make a http request for message "+qi.id);
                }
            }
        }
    }


    private QItem consume(String reserved) {
        /*
         * only one thread should consume data from the queue
         */
        // block until there is data
        QItem qi = null;
        while (qi == null && !shutdown) {
            try {
                qi = q.take();
            } catch (InterruptedException e) {
                //TODO: what to do?
            }
        }
        if (qi != null) {
            // process the data (well...looks like there is nothing that needs to be done)
        }
        // process possible overflow
        if (overflow != null) {
            // this should fit in the queue now, as we just took one item from there
            if (q.offer(overflow)) {
                overflow = null;
            }
        }
        return qi;
    }

}
