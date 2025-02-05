/*
 * Copyright 2019-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.selenium;

import java.net.URL;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.vividus.selenium.manager.GenericWebDriverManager;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;

public class RemoteWebDriverFactory implements IRemoteWebDriverFactory
{
    private final boolean useW3C;

    public RemoteWebDriverFactory(boolean useW3C)
    {
        this.useW3C = useW3C;
    }

    @Override
    public RemoteWebDriver getRemoteWebDriver(URL url, Capabilities capabilities)
    {
        /* Selenium 4 declares that it only supports W3C and nevertheless still writes  JWP's "desiredCapabilities" into
        "createSession" JSON. Appium Java client eliminates that: https://github.com/appium/java-client/pull/1537.
        But still some clouds (e.g. SmartBear CrossBrowserTesting) are not prepared for W3c, so we should avoid
        using Appium drivers to create sessions for web tests. */
        if (useW3C)
        {
            if (GenericWebDriverManager.isIOS(capabilities) || GenericWebDriverManager.isTvOS(capabilities))
            {
                return new IOSDriver(url, capabilities);
            }
            else if (GenericWebDriverManager.isAndroid(capabilities))
            {
                return new AndroidDriver(url, capabilities);
            }
        }
        return new RemoteWebDriver(url, capabilities);
    }
}
