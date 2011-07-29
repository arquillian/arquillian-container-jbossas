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

import javax.annotation.Resource;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * JBossASIntegrationTestCase
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @author Davide D'Alto
 * @version $Revision: $
 */
@RunWith(Arquillian.class)
public class JBossASIntegrationWarTestCase
{
   @Deployment
   public static WebArchive createDeployment() throws Exception 
   {
      return ShrinkWrap.create(WebArchive.class);
   }
   
   @Resource(mappedName = "UserTransaction")
   private UserTransaction transaction;
   
   @Test
   public void shouldBeAbleToInjectAInContainerResource() throws Exception 
   {
      Assert.assertNotNull(
            "Verify that the Resource has been injected",
            transaction);
      
   }
}
