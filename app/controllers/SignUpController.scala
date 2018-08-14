package controllers

import java.time.ZonedDateTime
import javax.inject._

import com.github.t3hnar.bcrypt._
import forms.SignUp
import models.User
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{ I18nSupport, Messages }
import play.api.mvc._
import play.api.{ Configuration, Logger }
import services.UserService

@Singleton
class SignUpController @Inject()(userService: UserService,
                                 components: ControllerComponents,
                                 config: Configuration)
  extends AbstractController(components)
    with I18nSupport {

  private val salt = config.get[String]("password.salt")

  private val signUpForm: Form[SignUp] = Form {
    mapping(
      "name"     -> nonEmptyText,
      "email"    -> email,
      "password" -> nonEmptyText,
      "confirm"  -> nonEmptyText
    )(SignUp.apply)(SignUp.unapply)
      .verifying("PasswordInvalid", form => form.password == form.confirm)
  }

  def index: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.signup.register(signUpForm))
  }

  def register: Action[AnyContent] = Action { implicit request =>
    signUpForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(views.html.signup.register(formWithErrors)), { signUp =>
          val now            = ZonedDateTime.now()
          val hashedPassword = signUp.password.bcrypt(salt)
          val user           = User(None, signUp.name, signUp.email, hashedPassword, now, now)
          userService
            .create(user)
            .map { _ =>
              Redirect(routes.HomeController.index())
                .flashing("success" -> Messages("SignUpSucceeded"))
            }
            .recover {
              case e: Exception =>
                Logger.error(s"occurred error", e)
                Redirect(routes.HomeController.index())
                  .flashing("failure" -> Messages("InternalError"))
            }
            .getOrElse(InternalServerError(Messages("InternalError")))
        }
      )
  }

}