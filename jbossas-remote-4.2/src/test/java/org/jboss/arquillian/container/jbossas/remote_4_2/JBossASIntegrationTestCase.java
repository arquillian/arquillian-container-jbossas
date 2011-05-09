/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.container.jbossas.remote_4_2;

import javax.ejb.EJB;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.container.jbossas.remote_4_2.ejb.MyEjb;
import org.jboss.arquillian.container.jbossas.remote_4_2.ejb.MyEjbBean;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * JBossASIntegrationTestCase
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
@Ignore
@RunWith(Arquillian.class)
public class JBossASIntegrationTestCase
{
   @Deployment
   public static EnterpriseArchive createDeployment() throws Exception 
   {
      return ShrinkWrap.create(EnterpriseArchive.class, "test.ear")
               .addAsModule(
                     ShrinkWrap.create(JavaArchive.class, "test.jar")
                        .addClasses(
                              JBossASIntegrationTestCase.class,
                              MyEjb.class, MyEjbBean.class)
                         )
               .setApplicationXML(
                     new StringAsset(
                           "<application>" +
                              "<module><ejb>test.jar</ejb></module>" +
                           "</application>"));
   }
   
   @EJB
   private MyEjb instanceVariable;
   
   @Test
   public void shouldBeAbleToInjectEJBAsInstanceVariable() throws Exception 
   {
      Assert.assertNotNull(
            "Verify that the Bean has been injected",
            instanceVariable);
      
      Assert.assertEquals("aslak", instanceVariable.getName());
   }
}