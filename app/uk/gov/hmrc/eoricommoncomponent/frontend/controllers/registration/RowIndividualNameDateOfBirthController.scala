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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration

import javax.inject.{Inject, Singleton}
import play.api.Application
import play.api.mvc.{Action, _}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{DetermineReviewPageController, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{CdsController, FeatureFlags}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.thirdCountryIndividualNameDateOfBirthForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RowIndividualNameDateOfBirthController @Inject() (
  override val currentApp: Application,
  override val authConnector: AuthConnector,
  subscriptionDetailsService: SubscriptionDetailsService,
  mcc: MessagesControllerComponents,
  rowIndividualNameDob: row_individual_name_dob
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) with FeatureFlags {

  def form(organisationType: String, service: Service, journey: Journey.Value): Action[AnyContent] =
    ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUser =>
      assertOrganisationTypeIsValid(organisationType)
      Future.successful(
        Ok(rowIndividualNameDob(thirdCountryIndividualNameDateOfBirthForm, organisationType, service, journey, false))
      )
    }

  def reviewForm(organisationType: String, service: Service, journey: Journey.Value): Action[AnyContent] =
    ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUser =>
      assertOrganisationTypeIsValid(organisationType)
      subscriptionDetailsService.cachedNameDobDetails flatMap {
        case Some(NameDobMatchModel(firstName, middleName, lastName, dateOfBirth)) =>
          val form = thirdCountryIndividualNameDateOfBirthForm.fill(
            IndividualNameAndDateOfBirth(firstName, middleName, lastName, dateOfBirth)
          )
          Future.successful(Ok(rowIndividualNameDob(form, organisationType, service, journey, true)))
        case _ => Future.successful(Redirect(SecuritySignOutController.signOut(journey)))
      }
    }

  def submit(
    isInReviewMode: Boolean,
    organisationType: String,
    service: Service,
    journey: Journey.Value
  ): Action[AnyContent] =
    ggAuthorisedUserWithEnrolmentsAction { implicit request => implicit loggedInUser: LoggedInUser =>
      assertOrganisationTypeIsValid(organisationType)
      thirdCountryIndividualNameDateOfBirthForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(
            BadRequest(rowIndividualNameDob(formWithErrors, organisationType, service, journey, isInReviewMode))
          ),
        form => submitDetails(isInReviewMode, form, organisationType, service, journey)
      )
    }

  private def assertOrganisationTypeIsValid(cdsOrganisationType: String): Unit =
    require(
      formsByOrganisationTypes contains cdsOrganisationType,
      message = s"Invalid organisation type '$cdsOrganisationType'."
    )

  private lazy val formsByOrganisationTypes =
    Seq(CdsOrganisationType.ThirdCountryIndividualId, CdsOrganisationType.ThirdCountrySoleTraderId)

  private def submitDetails(
    isInReviewMode: Boolean,
    formData: IndividualNameAndDateOfBirth,
    organisationType: String,
    service: Service,
    journey: Journey.Value
  )(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    val nameDobMatchModel =
      NameDobMatchModel(formData.firstName, formData.middleName, formData.lastName, formData.dateOfBirth)

    subscriptionDetailsService.cacheNameDobDetails(nameDobMatchModel) map { _ =>
      (isInReviewMode, rowHaveUtrEnabled) match {
        case (true, _)      => Redirect(DetermineReviewPageController.determineRoute(service, journey))
        case (false, true)  => Redirect(DoYouHaveAUtrNumberController.form(organisationType, service, journey, false))
        case (false, false) => Redirect(SixLineAddressController.showForm(false, organisationType, service, journey))
      }
    }
  }

}