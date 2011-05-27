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
package org.jboss.arquillian.container.jbossas.managed_4_2;

import javax.ejb.EJB;

import org.jboss.arquillian.container.jbossas.managed_4_2.ejb.MyEjb;
import org.jboss.arquillian.container.jbossas.managed_4_2.ejb.MyEjbBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.ee.application.ApplicationDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * JBossASIntegrationClientTestCase
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @author Davide D'Alto
 * @version $Revision: $
 */
@RunWith(Arquillian.class)
public class JBossASIntegrationMultipleTargetTestCase
{
   @Deployment(name = "dep1", order = 1)
   @TargetsContainer("jboss-i1")
   public static EnterpriseArchive createDeployment1() throws Exception
   {
      return createDeployment();
   }
   
   @Deployment(name = "dep2", order = 2)
   @TargetsContainer("jboss-i2")
   public static EnterpriseArchive createDeployment2() throws Exception
   {
      return createDeployment();
   }
   
   private static EnterpriseArchive createDeployment() throws Exception 
   {
      String applicationXml = Descriptors.create(ApplicationDescriptor.class, "application.xml")
            .ejbModule("test.jar").exportAsString();
      
      return ShrinkWrap.create(EnterpriseArchive.class, "test.ear")
               .addAsModule(
                     ShrinkWrap.create(JavaArchive.class, "test.jar")
                        .addClasses(
                              JBossASIntegrationMultipleTargetTestCase.class,
                              MyEjb.class, MyEjbBean.class)
                         )
                // we need to manually add the applications.xml file to the EAR
               .setApplicationXML(new StringAsset(applicationXml));
   }
   
   @EJB
   private MyEjb instanceVariable;

   @Test
   @OperateOnDeployment("dep1")
   public void shouldBeAbleToInjectEJBAsInstanceVariable() throws Exception 
   {
      Assert.assertNotNull(
            "Verify that the Bean has been injected",
            instanceVariable);
      
      Assert.assertEquals("aslak", instanceVariable.getName());
   }

   @Test
   @OperateOnDeployment("dep2")
   public void shouldBeAbleToInjectEJBAsInstanceVariableInOtherContainer() throws Exception 
   {
      Assert.assertNotNull(
            "Verify that the Bean has been injected",
            instanceVariable);
      
      Assert.assertEquals("aslak", instanceVariable.getName());
   }
}
