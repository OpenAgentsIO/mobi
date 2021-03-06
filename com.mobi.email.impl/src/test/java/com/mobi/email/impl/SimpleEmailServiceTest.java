package com.mobi.email.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import com.mobi.server.api.Mobi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/*-
 * #%L
 * com.mobi.email.impl
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 - 2018 iNovex Information Systems, Inc.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
public class SimpleEmailServiceTest {

    private SimpleEmailService es;
    private Map<String, Object> config;
    private SimpleSmtpServer smtpServer;
    private URL templatePath;

    private static final String SUBJECT_LINE = "This is a subject line.";
    private static final String TEXT_MESSAGE = "Hello, world.";
    private static final String HTML_MESSAGE = "<tr><td><p>" + TEXT_MESSAGE + "</p></td></tr>";
    private static final String TO_EMAIL_ADDRESS = "mobiemailtestuser@gmail.com";

    @Mock
    private Mobi mobi;

    @Mock
    private Bundle bundle;
    
    @Mock
    private BundleContext bundleContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mobi.getHostName()).thenReturn("https://localhost:8443/");
        templatePath = SimpleEmailService.class.getResource("/emailTemplate.html");
        when(bundle.getEntry(any(String.class))).thenReturn(templatePath);
        when(bundleContext.getBundle()).thenReturn(bundle);
        URL logoResource = SimpleEmailServiceTest.class.getResource("/mobi-primary-logo-cropped.png");
        when(bundle.getResource(any())).thenReturn(logoResource);

        es = new SimpleEmailService();
        es.setMobiServer(mobi);

        smtpServer = SimpleSmtpServer.start(SimpleSmtpServer.AUTO_SMTP_PORT);

        config = new HashMap<>();
        config.put("emailAddress", TO_EMAIL_ADDRESS);
        config.put("emailPassword", "Don't4get");
        config.put("port", smtpServer.getPort());
        config.put("smtpServer", "localhost");
        config.put("security", "");
        config.put("imageBasePath", "https://mobi.inovexcorp.com");
        config.put("emailTemplate", "emailTemplate.html");

        System.setProperty("karaf.etc", SimpleEmailServiceTest.class.getResource("/").getPath());

        Method m = es.getClass().getDeclaredMethod("activate", BundleContext.class, Map.class);
        m.setAccessible(true);
        m.invoke(es, bundleContext, config);
        assertNotNull(es);
    }

    @After
    public void tearDown() throws Exception {
        smtpServer.stop();
    }

    @Test
    public void sendSimpleEmailTest() throws Exception {
        CompletableFuture<Set<String>> cf = es.sendSimpleEmail(SUBJECT_LINE, TEXT_MESSAGE, TO_EMAIL_ADDRESS);
        Set<String> failedEmails = cf.get();
        assertEquals(0, failedEmails.size());

        List<SmtpMessage> emails = smtpServer.getReceivedEmails();
        assertEquals(1, emails.size());
        SmtpMessage email = emails.get(0);
        assertEquals(SUBJECT_LINE, email.getHeaderValue("Subject"));
        assertEquals(TO_EMAIL_ADDRESS, email.getHeaderValue("From"));
        assertTrue(email.getBody().contains(TEXT_MESSAGE));
    }

    @Test
    public void sendSimpleEmailInvalidAddressTest() throws Exception {
        CompletableFuture<Set<String>> cf = es.sendSimpleEmail(SUBJECT_LINE, TEXT_MESSAGE, "badAddress");
        Set<String> failedEmails = cf.get();
        assertEquals(1, failedEmails.size());
        assertEquals("badAddress", failedEmails.iterator().next());
    }

    @Test
    public void sendSimpleEmailInvalidAndValidAddressTest() throws Exception {
        CompletableFuture<Set<String>> cf = es.sendSimpleEmail(SUBJECT_LINE, TEXT_MESSAGE, "badAddress", TO_EMAIL_ADDRESS);
        Set<String> failedEmails = cf.get();
        assertEquals(1, failedEmails.size());
        assertEquals("badAddress", failedEmails.iterator().next());
    }

    @Test(expected = ExecutionException.class)
    public void sendSimpleEmailInvalidPortTest() throws Exception {
        config.replace("port", 1);
        Method m = es.getClass().getDeclaredMethod("modified", BundleContext.class, Map.class);
        m.setAccessible(true);
        m.invoke(es, bundleContext, config);

        CompletableFuture<Set<String>> cf = es.sendSimpleEmail(SUBJECT_LINE, TEXT_MESSAGE, TO_EMAIL_ADDRESS);
        cf.get();
    }

    @Test
    public void sendSimpleEmailAbsoluteTemplateTest() throws Exception {
        config.replace("emailTemplate", URLDecoder.decode(templatePath.getPath(), "UTF-8"));
        Method m = es.getClass().getDeclaredMethod("modified", BundleContext.class, Map.class);
        m.setAccessible(true);
        m.invoke(es, bundleContext, config);

        CompletableFuture<Set<String>> cf = es.sendSimpleEmail(SUBJECT_LINE, TEXT_MESSAGE, TO_EMAIL_ADDRESS);
        Set<String> failedEmails = cf.get();
        assertEquals(0, failedEmails.size());

        List<SmtpMessage> emails = smtpServer.getReceivedEmails();
        assertEquals(1, emails.size());
        SmtpMessage email = emails.get(0);
        assertEquals(SUBJECT_LINE, email.getHeaderValue("Subject"));
        assertEquals(TO_EMAIL_ADDRESS, email.getHeaderValue("From"));
        assertTrue(email.getBody().contains(TEXT_MESSAGE));
    }

    @Test
    public void sendEmailTest() throws Exception {
        CompletableFuture<Set<String>> cf = es.sendEmail(SUBJECT_LINE, HTML_MESSAGE, TO_EMAIL_ADDRESS);
        assertEquals(0, cf.get().size());

        List<SmtpMessage> emails = smtpServer.getReceivedEmails();
        assertEquals(1, emails.size());
        SmtpMessage email = emails.get(0);
        assertEquals(SUBJECT_LINE, email.getHeaderValue("Subject"));
        assertEquals(TO_EMAIL_ADDRESS, email.getHeaderValue("From"));
        assertTrue(email.getBody().contains(TEXT_MESSAGE));
    }

    @Test
    public void sendEmailSingleInvalidAddressTest() throws Exception {
        CompletableFuture<Set<String>> cf = es.sendEmail(SUBJECT_LINE, HTML_MESSAGE, "badAddress");
        Set<String> failedEmails = cf.get();
        assertEquals(1, failedEmails.size());
        assertEquals("badAddress", failedEmails.iterator().next());
    }

    @Test
    public void sendEmailInvalidAndValidAddressTest() throws Exception {
        CompletableFuture<Set<String>> cf = es.sendEmail(SUBJECT_LINE, HTML_MESSAGE, "badAddress", TO_EMAIL_ADDRESS);
        Set<String> failedEmails = cf.get();
        assertEquals(1, failedEmails.size());
        assertEquals("badAddress", failedEmails.iterator().next());
    }

    @Test(expected = ExecutionException.class)
    public void sendEmailInvalidPortTest() throws Exception {
        config.replace("port", 1);
        Method m = es.getClass().getDeclaredMethod("modified", BundleContext.class, Map.class);
        m.setAccessible(true);
        m.invoke(es, bundleContext, config);

        CompletableFuture<Set<String>> cf = es.sendEmail(SUBJECT_LINE, HTML_MESSAGE, TO_EMAIL_ADDRESS);
        cf.get();
    }
}
