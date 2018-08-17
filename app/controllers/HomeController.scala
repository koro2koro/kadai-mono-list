package controllers

import javax.inject._

import jp.t2v.lab.play2.auth.OptionalAuthElement
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.UserService

@Singleton
class HomeController @Inject()(val userService: UserService, components: ControllerComponents)
  extends AbstractController(components)
    with I18nSupport
    with AuthConfigSupport
    with OptionalAuthElement { // `AuthConfigSupport`,`OptionalAuthElement`をミックスイン

  def index: Action[AnyContent] = StackAction { implicit request => // `index`アクションを`Action`から`StackAction`に変更
    Ok(views.html.index(loggedIn)) // `views.html.index()`から`views.html.index(loggedIn)`に変更
  }

}