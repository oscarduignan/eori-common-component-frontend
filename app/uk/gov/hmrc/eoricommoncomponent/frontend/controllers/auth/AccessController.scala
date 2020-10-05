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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth

import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.auth.core.{AffinityGroup, CredentialRole, Enrolment, User}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{routes, JourneyTypeFromUrl}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.views.ServiceName.service

import scala.concurrent.Future

trait AccessController extends JourneyTypeFromUrl with AllowlistVerification {

  def permitUserOrRedirect(
    affinityGroup: Option[AffinityGroup],
    credentialRole: Option[CredentialRole],
    enrolments: Set[Enrolment]
  )(action: Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {

    def hasEnrolment(implicit request: Request[AnyContent]): Boolean =
      Service.serviceFromRequest.exists(service => enrolments.exists(_.key.equalsIgnoreCase(service.enrolmentKey)))

    def isPermittedUserType: Boolean =
      affinityGroup match {
        case Some(Agent)        => false
        case Some(Organisation) => credentialRole.fold(false)(cr => cr == User)
        case _                  => true
      }

    (isPermittedUserType, hasEnrolment) match {
      case (true, false) => action
      case (true, true) =>
        Future.successful(Redirect(routes.EnrolmentAlreadyExistsController.enrolmentAlreadyExists(service)))
      case (false, _) =>
        Future.successful(Redirect(routes.YouCannotUseServiceController.page(service, journeyFromUrl)))
    }
  }

}
