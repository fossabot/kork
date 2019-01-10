/*
 * Copyright 2019 Armory, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.config.secrets;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;


import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class SecretManagerTest {

  @Mock
  SecretEngineRegistry secretEngineRegistry;

  @Mock
  SecretEngine secretEngine;

  SecretManager secretManager = new SecretManager();

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(secretEngineRegistry.getEngine("s3")).thenReturn(secretEngine);
    when(secretEngine.identifier()).thenReturn("s3");
    secretManager.setSecretEngineRegistry(secretEngineRegistry);
  }

  @Test
  public void decryptTest() throws SecretDecryptionException {
    String secretConfig = "encrypted:s3!paramName:paramValue";
    when(secretEngine.decrypt(any())).thenReturn("test");
    assertEquals("test", secretManager.decrypt(secretConfig));
  }

  @Test
  public void decryptSecretEngineNotFound() throws SecretDecryptionException {
    when(secretEngineRegistry.getEngine("does-not-exist")).thenReturn(null);
    String secretConfig = "encrypted:does-not-exist!paramName:paramValue";
    exceptionRule.expect(InvalidSecretFormatException.class);
    exceptionRule.expectMessage("Secret Engine does not exist: does-not-exist");
    secretManager.decrypt(secretConfig);
  }

  @Test
  public void decryptInvalidParams() throws SecretDecryptionException {
    doThrow(InvalidSecretFormatException.class).when(secretEngine).validate(any());
    String secretConfig = "encrypted:s3!paramName:paramValue";
    exceptionRule.expect(InvalidSecretFormatException.class);
    secretManager.decrypt(secretConfig);
  }

  @Test
  public void decryptFile() throws SecretDecryptionException, IOException {
    String secretConfig = "encrypted:s3!paramName:paramValue";
    when(secretEngine.decrypt(any())).thenReturn("test");
    String tempFilPath = secretManager.decryptFile(secretConfig);
    assertTrue(tempFilPath.matches(".*s3.*.secret$"));
    BufferedReader reader = new BufferedReader(new FileReader(tempFilPath));
    assertEquals("test", reader.readLine());
    reader.close();
  }

  @Test
  public void decryptFileSecretEngineNotFound() throws SecretDecryptionException {
    when(secretEngineRegistry.getEngine("does-not-exist")).thenReturn(null);
    String secretConfig = "encrypted:does-not-exist!paramName:paramValue";
    exceptionRule.expect(InvalidSecretFormatException.class);
    exceptionRule.expectMessage("Secret Engine does not exist: does-not-exist");
    secretManager.decryptFile(secretConfig);
  }

  @Test
  public void decryptFileInvalidParams() throws SecretDecryptionException {
    doThrow(InvalidSecretFormatException.class).when(secretEngine).validate(any());
    String secretConfig = "encrypted:s3!paramName:paramValue";
    exceptionRule.expect(InvalidSecretFormatException.class);
    secretManager.decryptFile(secretConfig);
  }

  @Test
  public void decryptFileNoDiskSpace() throws SecretDecryptionException {
    SecretManager mockedSecretManager = mock(SecretManager.class);
    when(secretEngine.decrypt(any())).thenReturn("test");
    when(mockedSecretManager.decryptedFilePath(any(), any())).thenThrow(SecretDecryptionException.class);
    when(mockedSecretManager.decryptFile(any())).thenCallRealMethod();
    doCallRealMethod().when(mockedSecretManager).setSecretEngineRegistry(any());
    mockedSecretManager.setSecretEngineRegistry(secretEngineRegistry);
    exceptionRule.expect(SecretDecryptionException.class);

    String secretConfig = "encrypted:s3!paramName:paramValue";
    mockedSecretManager.decryptFile(secretConfig);
  }

}

