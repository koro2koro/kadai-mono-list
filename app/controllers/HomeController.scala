package controllers

import javax.inject._
import jp.t2v.lab.play2.auth.OptionalAuthElement
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{ItemService, UserService}

@Singleton
class HomeController @Inject()(val userService: UserService, val itemService: ItemService, components: ControllerComponents)
  extends AbstractController(components)
    with I18nSupport
    with AuthConfigSupport
    with OptionalAuthElement { // `AuthConfigSupport`,`OptionalAuthElement`をミックスイン

  def index: Action[AnyContent] = StackAction { implicit request => // `index`アクションを`Action`から`StackAction`に変更
    // 商品検索結果をビューに渡す
    itemService
      .getLatestItems()
      .map { items =>
        Ok(views.html.index(loggedIn, items))
      }
      .getOrElse(InternalServerError(Messages("InternalError")))
  }

}