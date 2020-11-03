/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package unit.util

import org.scalatest.{MustMatchers, WordSpec}
import uk.gov.hmrc.eoricommoncomponent.frontend.util.{InvalidUrlValueException, Require}

class RequireSpec extends WordSpec with MustMatchers {

  "Require requireThatUrlValue" should {

    "throw InvalidUrlValueException when requirement not met" in {

      val caught = intercept[InvalidUrlValueException] {
        Require.requireThatUrlValue(1 == 3, "Some Error")
      }
      caught.getMessage mustBe "invalid value: Some Error"

    }
  }
}