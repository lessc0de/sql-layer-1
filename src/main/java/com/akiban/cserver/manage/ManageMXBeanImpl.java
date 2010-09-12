package com.akiban.cserver.manage;

import java.util.List;

import com.akiban.cserver.CServer;
import com.akiban.cserver.CServerConfig;
import com.akiban.cserver.CServer.CapturedMessage;
import com.akiban.cserver.store.PersistitStore;

class ManageMXBeanImpl implements ManageMXBean {

    private final CServer cserver;
    private final CServerConfig config;

    protected ManageMXBeanImpl(final CServer cserver, final CServerConfig config) {
        this.cserver = cserver;
        this.config = config;
    }

    @Override
    public void ping() {
        return;
    }

    @Override
    public void shutdown() {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
        }.start();
    }

    @Override
    public int getNetworkPort() {
        String value = cserver.port();
        return Integer.parseInt(value);
    }

    @Override
    public int getJmxPort() {
        int jmxPort = Integer
                .getInteger("com.sun.management.jmxremote.port", 0);
        return jmxPort;
    }
    
    @Override
    public boolean isVerboseLoggingEnabled() {
        return cserver.getStore().isVerbose();
    }

    @Override
    public void disableVerboseLogging() {
        cserver.getStore().setVerbose(false);
    }

    @Override
    public void enableVerboseLogging() {
        cserver.getStore().setVerbose(true);
    }

    @Override
    public void displayCapturedMessages() {
        System.out.println();
        System.out.println("---->");
        final List<CapturedMessage> messages = cserver.getCapturedMessageList();
        for (final CapturedMessage message : messages) {
            System.out.println(message.toString(cserver.getRowDefCache()));
        }
        System.out.println("<----");
    }

    @Override
    public void setMaxCapturedMessageCount(int count) {
        cserver.setMaxCapturedMessageCound(count);
    }

    @Override
    public void startMessageCapture() {
        cserver.setMessageCaptureEnabled(true);
    }

    @Override
    public void stopMessageCapture() {
        cserver.setMessageCaptureEnabled(false);
    }

    @Override
    public void clearCapturedMessages() {
        cserver.clearCapturedMessages();
    }

    @Override
    public boolean isDeferIndexesEnabled() {
        return cserver.getStore().isDeferIndexes();
    }

    @Override
    public void setDeferIndexes(final boolean defer) {
        ((PersistitStore) cserver.getStore()).setDeferIndexes(defer);
    }
    
    @Override
    public void buildIndexes(final String arg) {
        ((PersistitStore) cserver.getStore()).buildIndexes(arg);
    }
    
    @Override
    public void deleteIndexes(final String arg) {
        ((PersistitStore) cserver.getStore()).deleteIndexes(arg);
    }
    
    @Override
    public void flushIndexes() {
        ((PersistitStore) cserver.getStore()).flushIndexes();
    }
    
    // TODO - temporary
    @Override
    public String copyBackPages() {
        try {
            ((PersistitStore) cserver.getStore()).getDb().copyBackPages();
        } catch (Exception e) {
            return e.toString();
        }
        return "done";
    }
    
    
    

}
