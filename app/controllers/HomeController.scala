package controllers

import javax.inject._

import play.api.i18n.I18nSupport
import play.api.mvc._

@Singleton
class HomeController @Inject()(components: ControllerComponents)
  extends AbstractController(components)
    with I18nSupport {

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }
}