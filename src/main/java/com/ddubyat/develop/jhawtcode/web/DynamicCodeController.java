package com.ddubyat.develop.jhawtcode.web;

import com.ddubyat.develop.jhawtcode.dynamic.DynamicCodeInvocationHandler;
import com.ddubyat.develop.jhawtcode.dynamic.DynamicRuntimeCode;
import com.ddubyat.develop.jhawtcode.util.ClassCompilerUtil;
import com.ddubyat.develop.jhawtcode.util.PropertyUtil;
import com.ddubyat.develop.jhawtcode.util.ResourceUtil;
import jodd.util.ClassLoaderUtil;
import jodd.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class DynamicCodeController {

    @Autowired
    private ClassCompilerUtil classCompilerUtil;

    @Autowired
    private ResourceUtil resourceUtil;

    @Autowired
    private PropertyUtil propertyUtil;

    @Autowired
    private ApplicationContext applicationContext;

    private static String templateClassName = "DynamicCodeFrom";
    private static String templateClassPathPrefix = "com/ddubyat/develop/jhawtcode/dynamic/";
    private static String templateClassNamePostfix = templateClassName + ".java";

    @ResponseBody
    @RequestMapping(value = "/jhawtcode", method = {RequestMethod.GET})
    public String version() {
        if(!propertyUtil.canHawtTheCode()) {
            return (new String(""));
        }

        return "0.0.3-DEMO";
    }

    @ResponseBody
    @RequestMapping(value = "/jhawtcode/dynacode", method = {RequestMethod.POST})
    public String runCode(HttpServletRequest request, HttpServletResponse response, @RequestParam(required = true, value = "code") String code, @RequestParam(required = false, value = "imports") String imports, @RequestParam(required = false, value = "globals") String globals, @RequestParam(required = false, value = "methods") String methods) throws IOException, ClassNotFoundException {
        if(!propertyUtil.canHawtTheCode()) {
            return (new String(""));
        }

        String templateClass = resourceUtil.readLocalResource("classpath:" + templateClassPathPrefix + templateClassNamePostfix);
        templateClass = templateClass.replace(templateClassName, templateClassName + "Console");
        templateClass = templateClass.replace("//JHC-CODE//", code + System.lineSeparator());

        if(code.indexOf("jhc.print") > 1 || code.indexOf("jhc.print") == -1 || code.length() > 60 || code.contains("exit") || code.contains("sleep") || code.contains("Thread") || code.contains("halt") || code.contains("System")) {
            return "Unacceptable JAVA Input, read the rules for the demo";
        }

        String compilationError = "";
        URLClassLoader ucl = (URLClassLoader) Thread.currentThread().getContextClassLoader();
        Class newDynamicClass = null;

        try {
            newDynamicClass = classCompilerUtil.compileNewClass(templateClassName + "Console", templateClassPathPrefix, templateClass, ucl);
        } catch (Exception e) {
            compilationError = e.getLocalizedMessage();
        }

        if(newDynamicClass != null) {
            InvocationHandler handler = new DynamicCodeInvocationHandler(newDynamicClass);
            DynamicRuntimeCode proxy = (DynamicRuntimeCode) Proxy.newProxyInstance(DynamicRuntimeCode.class.getClassLoader(), new Class[]{DynamicRuntimeCode.class}, handler);
            try {
                String runtimeResponse = proxy.doCode(applicationContext, request, response);
                if(runtimeResponse == null) {
                    runtimeResponse = "null";
                }
                return runtimeResponse;
            } catch (Throwable t) {
                return "Runtime Exception: " + t.getLocalizedMessage();
            }
        }

        return compilationError;
    }

    /*@ResponseBody
    @RequestMapping(value = "/jhawtcode/dynaprop", method = {RequestMethod.POST})
    public String setProp(@RequestParam(required = true, value = "propkey") String propkey, @RequestParam(required = true, value = "value") String value) {
        if(!propertyUtil.canHawtTheCode()) {
            return (new String(""));
        }

        return propertyUtil.setProp(propkey, value);
    }

    @ResponseBody
    @RequestMapping(value = "/jhawtcode/dynajar", method = {RequestMethod.POST})
    public String loadJar(@RequestParam(required = true, value = "url") String url) throws MalformedURLException {
        if(!propertyUtil.canHawtTheCode()) {
            return (new String(""));
        }

        URLClassLoader ucl = (URLClassLoader) Thread.currentThread().getContextClassLoader();
        try {
            ClassLoaderUtil.addUrlToClassPath(new URL(url), ucl);
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
        return "Jar File Loaded into Classpath";
    }

    @ResponseBody
    @RequestMapping(value = "/jhawtcode/createJar", method = {RequestMethod.POST})
    public String createJar(@RequestParam(required = true, value = "name") String name, @RequestParam("file") MultipartFile file) {
        if(!propertyUtil.canHawtTheCode()) {
            return (new String(""));
        }

        if(!file.isEmpty()) {
            try {
                File jarFile = new File(System.getProperty("java.io.tmpdir") + "/" + name);
                byte[] bytes = file.getBytes();
                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(jarFile));
                stream.write(bytes);
                stream.close();
                return "File Uploaded\n" + loadJar("file:" + jarFile.getAbsolutePath());
            } catch (Exception e) {
                return "Upload Failed: " + e.getLocalizedMessage();
            }
        } else {
            return "Upload Failed: Empty";
        }
    }*/

}