package com.ddubyat.develop.jhawtcode.web;

import com.ddubyat.develop.jhawtcode.util.PropertyUtil;
import com.ddubyat.develop.jhawtcode.util.ResourceUtil;
import jodd.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.UUID;
import java.security.*;
import java.net.NetworkInterface;

/**
 * InternalResourceController is a Spring Controller that will serve up css and js for JHawtCode
 *
 * @author dwtalk
 * @version 1.0.0
 * @since 2014-07-15
 */
@Controller
public class InternalResourceController {

    private static Logger log = LoggerFactory.getLogger(InternalResourceController.class);
    private static String systemUUID = "";
    private static String appname = "";
    private static String username = "";
    private static String license = "";

    @Autowired
    private ResourceUtil resourceUtil;

    @Autowired
    private PropertyUtil propertyUtil;

    /**
     * Controller method to deliver application CSS
     *
     * @param response The http response object from a spring controller
     * @return GZip byte array of CSS code
     * @throws IOException
     */
    @ResponseBody
    @RequestMapping(value = "/jhawtcode/jhc.css", method = {RequestMethod.GET})
    public byte[] requestCSS(HttpServletResponse response) throws IOException {
        log.debug("Creating application CSS");

        response.setHeader("Content-Encoding", "gzip");
        response.setHeader("Server", "jhawtconsole");
        response.setContentType("text/css");
        response.setHeader("Cache-Control", "no-cache,no-store,must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", -1);

        if(!propertyUtil.canHawtTheCode()) {
            log.trace("Not delivering CSS, application not enabled");
            return (new String("")).getBytes();
        }

        String heightOriginal = "height: 200px;";
        String heightOverride = heightOriginal;
        String sysHeightProp = System.getProperty("jhawtcode.console.height");
        if(sysHeightProp != null && StringUtil.isNotEmpty(sysHeightProp) && isNumeric(sysHeightProp)) {
            heightOverride = heightOriginal.replaceAll("200", sysHeightProp);
            log.debug("Overriding console height to {}", heightOverride);
        }

        try {
            log.debug("Attempting to return CSS");
            return resourceUtil.gzipCompress(resourceUtil.readLocalResource("classpath:/css/jhawtcode.css").replaceAll(heightOriginal, heightOverride));
        } catch (IOException ioe) {
            log.debug("Unable to create CSS", ioe);
            return ioe.getLocalizedMessage().getBytes("UTF-8");
        }
    }

    /**
     * Controller method to deliver application JS
     *
     * @param response The http response object from a spring controller
     * @param request The http request object from a spring controller
     * @return GZip byte array of JS code
     * @throws IOException
     */
    @ResponseBody
    @RequestMapping(value = "/jhawtcode/jhc.js", method = {RequestMethod.GET})
    public byte[] requestJS(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.debug("Creating application JS");

        response.setHeader("Content-Encoding", "gzip");
        response.setHeader("Server", "jhawtconsole");
        response.setContentType("application/javascript");
        response.setHeader("Cache-Control", "no-cache,no-store,must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", -1);

        if(!propertyUtil.canHawtTheCode()) {
            log.trace("Not delivering CSS, application not enabled");
            return (new String("")).getBytes();
        }

        setTraceProps(request);

        String javascript = resourceUtil.readLocalResource(new String[]{"classpath:/js/jhawtcode.js"});
        javascript = javascript.replace("|systemUUID|", systemUUID);
        javascript = javascript.replace("|username|", username);
        javascript = javascript.replace("|license|", license);
        javascript = javascript.replace("|appname|", appname);

        try {
            log.debug("Attempting to return JS");
            return resourceUtil.gzipCompress(javascript);
        } catch (IOException ioe) {
            log.debug("Unable to create JS", ioe);
            return ioe.getLocalizedMessage().getBytes("UTF-8");
        }

    }

    private static void setTraceProps(HttpServletRequest request) {
        log.trace("Generating system properties");
        if(systemUUID.equalsIgnoreCase("")) {
            try {
                //generate from hardware
                Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
                while (nis.hasMoreElements()) {
                    NetworkInterface ni = nis.nextElement();
                    systemUUID += Arrays.toString(ni.getHardwareAddress());
                }
            } catch (Exception e) {
                //generate a uuid randomly for failures
                systemUUID = UUID.randomUUID().toString();
            }
            //md5 the uuid
            try {
                byte[] bytesOfMessage = systemUUID.getBytes("UTF-8");
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] thedigest = md.digest(bytesOfMessage);
                systemUUID = new BigInteger(1, thedigest).toString(16);
                log.trace("UUID Generated {}", systemUUID);
            } catch (Exception e) {
                systemUUID = "1234567890";
                log.trace("UUID Defaulted {}", systemUUID);
            }
        }

        log.trace("Generating app name");
        if(appname.equalsIgnoreCase("")) {
            if (request.getSession() != null && request.getSession().getServletContext() != null && request.getSession().getServletContext().getServletContextName() != null) {
                appname = request.getServletContext().getServletContextName();
                log.trace("Application Name Set to {}", appname);
            } else {
                appname = "noappname";
            }
            log.trace("Appname {}", appname);
        }

        log.trace("Generating username");
        if(username.equalsIgnoreCase("")) {
            if (System.getProperty("user.name") != null && StringUtil.isNotEmpty(System.getProperty("user.name"))) {
                username = System.getProperty("user.name");
                log.trace("Username Set to {}", username);
            } else {
                username = "nouser";
            }
            log.trace("Username {}", username);
        }

        log.trace("Generating license info");
        if(license.equalsIgnoreCase("")) {
            if (System.getProperty("jhawtcode.license") != null && StringUtil.isNotEmpty(System.getProperty("jhawtcode.license"))) {
                license = System.getProperty("jhawtcode.license");
            } else {
                license = "demo";
            }
            log.trace("License {}", license);
        }
    }

    private static boolean isNumeric(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }


}
