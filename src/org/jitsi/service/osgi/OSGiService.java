/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.service.osgi;

import android.app.*;
import android.content.res.*;
import android.content.*;
import android.support.v4.app.*;
import android.os.*;

import net.java.sip.communicator.util.*;

import org.jitsi.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.*;
import org.jitsi.impl.osgi.*;
import org.jitsi.service.configuration.*;

import java.beans.*;

/**
 * Implements an Android {@link Service} which (automatically) starts and stops
 * an OSGi framework (implementation).
 *
 * @author Lyubomir Marinov
 */
public class OSGiService
    extends Service
{
    /**
     * The ID of Jitsi notification icon
     */
    private static int GENERAL_NOTIFICATION_ID = R.string.app_name;

    /**
     * Name of config property that indicates whether foreground icon should be
     * displayed.
     */
    private static final String SHOW_ICON_PROPERTY_NAME
            = "org.jitsi.android.show_icon";

    /**
     * Indicates that Jitsi is running in foreground mode and it's icon is
     * constantly displayed.
     */
    private static boolean running_foreground = false;

    /**
     * Indicates if the service has been started and general notification
     * icon is available
     */
    private static boolean serviceStarted;

    /**
     * The very implementation of this Android <tt>Service</tt> which is split
     * out of the class <tt>OSGiService</tt> so that the class
     * <tt>OSGiService</tt> may remain in a <tt>service</tt> package and be
     * treated as public from the Android point of view and the class
     * <tt>OSGiServiceImpl</tt> may reside in an <tt>impl</tt> package and be
     * recognized as internal from the Jitsi point of view.
     */
    private final OSGiServiceImpl impl;

    /**
     * Initializes a new <tt>OSGiService</tt> implementation.
     */
    public OSGiService()
    {
        impl = new OSGiServiceImpl(this);
    }

    public IBinder onBind(Intent intent)
    {
        return impl.onBind(intent);
    }

    @Override
    public void onCreate()
    {
        impl.onCreate();
    }

    @Override
    public void onDestroy()
    {
        impl.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        int result = impl.onStartCommand(intent, flags, startId);

        return result;
    }

    /**
     * Method called by OSGi impl when start command completes.
     */
    public void onOSGiStarted()
    {
        if(isIconEnabled())
        {
            showIcon();
        }
        getConfig().addPropertyChangeListener(
                SHOW_ICON_PROPERTY_NAME,
                new PropertyChangeListener()
                {
                    @Override
                    public void propertyChange(PropertyChangeEvent event)
                    {
                        if(isIconEnabled())
                        {
                            showIcon();
                        }
                        else
                        {
                            hideIcon();
                        }
                    }
                });
        serviceStarted = true;
    }

    private ConfigurationService getConfig()
    {
        return ServiceUtils.getService(
                AndroidGUIActivator.bundleContext,
                ConfigurationService.class);
    }

    private boolean isIconEnabled()
    {
        return getConfig().getBoolean(SHOW_ICON_PROPERTY_NAME, true);
    }

    /**
     * Start the service in foreground and creates shows general notification
     * icon.
     */
    private void showIcon()
    {
        //The intent to launch when the user clicks the expanded notification
        Intent intent
            = new Intent(this, JitsiApplication.getHomeScreenActivityClass());
        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendIntent =
                PendingIntent.getActivity(this, 0, intent, 0);

        Resources res = getResources();
        String title = res.getString(R.string.app_name);

        NotificationCompat.Builder nBuilder
                = new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.notificationicon);
        nBuilder.setContentIntent(pendIntent);

        Notification notice = nBuilder.build();
        notice.flags |= Notification.FLAG_NO_CLEAR;

        this.startForeground(GENERAL_NOTIFICATION_ID, notice);
        running_foreground = true;
    }

    /**
     * Stops the foreground service and hides general notification icon
     */
    public void stopForegroundService()
    {
        serviceStarted = false;
        hideIcon();
    }

    private void hideIcon()
    {
        if(running_foreground)
        {
            stopForeground(true);
            running_foreground = false;
        }
    }

    /**
     * Returns general notification ID that can be used to post notification
     * bound to our global icon
     * 
     * @return the notification ID greater than 0 or -1 if service is not 
     *  running
     */
    public static int getGeneralNotificationId()
    {
        if(serviceStarted && running_foreground)
        {
            return GENERAL_NOTIFICATION_ID;
        }
        return -1;
    }
}
