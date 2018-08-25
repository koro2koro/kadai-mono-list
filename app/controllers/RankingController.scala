package controllers

import javax.inject.{Inject, Singleton}

import jp.t2v.lab.play2.auth.AuthenticationElement
import models.{Item, WantHaveType}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{ItemService, UserService}

import scala.util.Try

@Singleton
class RankingController @Inject()(val userService: UserService,
                                  val itemService: ItemService,
                                  components: ControllerComponents)
  extends AbstractController(components)
    with I18nSupport
    with AuthConfigSupport
    with AuthenticationElement {

  def want: Action[AnyContent] = StackAction { implicit request =>
    val user = loggedIn
    getItemsByRanking(WantHaveType.Want).map { items =>
        Ok(views.html.ranking.want(user, items.map { case (item, count) => (item, Some(count)) }))
      }.getOrElse(InternalServerError(Messages("InternalError")))
  }

  def have: Action[AnyContent] = StackAction { implicit request =>
    val user = loggedIn
    getItemsByRanking(WantHaveType.Have).map { items =>
      Ok(views.html.ranking.have(user, items.map { case (item, count) => (item, Some(count)) }))
    }.getOrElse(InternalServerError(Messages("InternalError")))
  }

  private def getItemsByRanking(`type`: WantHaveType.Value): Try[Seq[(Item, Int)]] = {
    itemService.getItemsByRanking(`type`).map { itemWithCounts =>
        val ids = itemWithCounts.map(_._1.id.get)
        val items = Item.allAssociations.findAllByIds(ids: _*).map(e => e.id.get -> e).toMap
        itemWithCounts.map { e =>
          (items(e._1.id.get), e._2)
        }
      }
  }

}