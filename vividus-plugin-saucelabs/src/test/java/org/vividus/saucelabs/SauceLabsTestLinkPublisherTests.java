/*
 * Copyright 2019-2023 the original author or authors.
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

package org.vividus.saucelabs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.vividus.selenium.cloud.AbstractCloudTestLinkPublisher.GetCloudTestUrlException;

class SauceLabsTestLinkPublisherTests
{
    @ParameterizedTest
    @CsvSource({
            "eu,             app.eu-central-1.saucelabs.com",
            "EU_CENTRAL,     app.eu-central-1.saucelabs.com",
            "US,             app.saucelabs.com",
            "us_west,        app.saucelabs.com",
            "us_east,        app.us-east-1.saucelabs.com",
            "APAC_SOUTHEAST, app.apac-southeast-1.saucelabs.com"
    })
    void shouldReturnSessionUrl(String dataCenter, String host) throws GetCloudTestUrlException
    {
        var linkPublisher = new SauceLabsTestLinkPublisher(new DataCenterProvider(dataCenter), null, null, null);
        assertEquals("https://" + host + "/tests/session-id", linkPublisher.getCloudTestUrl("session-id"));
    }
}
