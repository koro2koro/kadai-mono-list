package services

import java.time.ZonedDateTime

import javax.inject.{Inject, Singleton}
import akka.actor.ActorSystem
import com.github.j5ik2o.rakutenApi.itemSearch.{ImageFlagType, RakutenItemSearchAPI, RakutenItemSearchAPIConfig, Item => RakutenItem}
import models.{Item, ItemUser}
import play.api.Configuration
import play.api.libs.concurrent.ActorSystemProvider
import scalikejdbc.{DBSession, sqls}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

@Singleton
class ItemServiceImpl @Inject()(configuration: Configuration, actorSystemProvider: ActorSystemProvider)
  extends ItemService {

  private implicit val system: ActorSystem = actorSystemProvider.get

  import system.dispatcher

  private val config = RakutenItemSearchAPIConfig(
    endPoint = configuration.get[String]("rakuten.endPoint"),
    timeoutForToStrict = configuration.get[Int]("rakuten.timeoutForToStrictInSec") seconds,
    applicationId = configuration.get[String]("rakuten.applicationId"),
    affiliateId = configuration.getOptional[String]("rakuten.affiliateId")
  )

  private val rakutenItemSearchAPI = new RakutenItemSearchAPI(config)

  override def searchItems(keywordOpt: Option[String]): Future[Seq[Item]] = {
    keywordOpt
      .map { keyword =>
        rakutenItemSearchAPI
          .searchItems(
            keyword = Some(keyword),
            hits = Some(20),
            imageFlag = Some(ImageFlagType.HasImage)
          )
          .map(_.Items.map(convertToItem))
      }
      .getOrElse(Future.successful(Seq.empty))
  }

  override def getItemByCode(itemCode: String)(implicit dbSession: DBSession): Future[Option[Item]] = Future {
    Item.allAssociations.findBy(sqls.eq(Item.defaultAlias.code, itemCode))
  }

  override def getItemAndCreateByCode(itemCode: String)(implicit dbSession: DBSession): Future[Item] = {
    getItemByCode(itemCode).flatMap {
      case Some(item) =>
        Future.successful(item)
      case None =>
        searchItemByItemCode(itemCode).map { item =>
          val id = create(item).get
          item.copy(id = Some(id))
        }
    }
  }

  private def convertToItem(rakutenItem: RakutenItem): Item = {
    Item.allAssociations
      .findBy(sqls.eq(Item.defaultAlias.code, rakutenItem.value.itemCode))
      .getOrElse {
        createItemFromRakutenItem(rakutenItem)
      }
  }

  private def searchItemByItemCode(itemCode: String): Future[Item] =
    rakutenItemSearchAPI.searchItems(itemCode = Some(itemCode)).map(_.Items.head).map(createItemFromRakutenItem)

  private def createItemFromRakutenItem(rakutenItem: RakutenItem): Item = {
    val now = ZonedDateTime.now()
    Item(
      id = None,
      code = rakutenItem.value.itemCode,
      name = rakutenItem.value.itemName,
      url = rakutenItem.value.itemUrl.toString,
      imageUrl = rakutenItem.value.mediumImageUrls.head.value.toString.replace("?_ex=128x128", ""),
      price = rakutenItem.value.itemPrice.toInt,
      createAt = now,
      updateAt = now
    )
  }

  private def create(item: Item)(implicit dbSession: DBSession): Try[Long] = Try {
    Item.create(item)
  }

  override def getItemsByUserId(userId: Long)(implicit dbSession: DBSession): Try[Seq[Item]] = Try {
    Item.allAssociations.findAllBy(sqls.eq(ItemUser.defaultAlias.userId, userId))
  }

  override def getItemById(itemId: Long)(implicit dbSession: DBSession): Future[Option[Item]] = Future {
    Item.allAssociations.findById(itemId)
  }

  override def getLatestItems(limit: Int = 20): Try[Seq[Item]] = Try {
    Item.allAssociations
      .findAllWithLimitOffset(limit, orderings = Seq(Item.defaultAlias.updateAt.desc))
      .toVector
  }

}