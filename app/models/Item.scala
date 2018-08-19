package models

import java.time.ZonedDateTime

import scalikejdbc._
//import jsr310._
import skinny.orm.feature.CRUDFeatureWithId
import skinny.orm.feature.associations.HasManyAssociation
import skinny.orm.{Alias, SkinnyCRUDMapper}

case class Item(id: Option[Long],
                code: String,
                name: String,
                url: String,
                imageUrl: String,
                price: Int,
                createAt: ZonedDateTime,
                updateAt: ZonedDateTime,
                users: Seq[User] = Seq.empty,
                wantUsers: Seq[User] = Seq.empty)

object Item extends SkinnyCRUDMapper[Item] {

  lazy val allAssociations: CRUDFeatureWithId[Long, Item] = joins(allUsersRef, wantUsersRef)

  /*
  through：中間テーブルを表すオブジェクト
  many：1対多の多の方のテーブル
  merge：識別子と関連するテーブルを結合させている
   */
  lazy val allUsersRef: HasManyAssociation[Item] = hasManyThrough[User](
    through = ItemUser,
    many = User,
    merge = (item, users) => item.copy(users = users)
  )

  /*
  through：中間テーブルとエイリアスのタプルを指定
  throughOn：ジョインする条件を指定(識別子ありのモデル)
  many：1対多の多の方のテーブルとエイリアスをタプルで指定
  on：ジョインする条件を指定(識別子で無い方のモデル)
  merge：識別子を指定して関連テーブルと結合
   */
  lazy val wantUsersRef: HasManyAssociation[Item] = hasManyThrough[ItemUser, User](
    through = ItemUser -> ItemUser.defaultAlias,
    throughOn = (item: Alias[Item], itemUser: Alias[ItemUser]) => sqls.eq(item.id, itemUser.itemId),
    many = User -> User.createAlias("i_in_u_want"),
    on = (itemUser: Alias[ItemUser], user: Alias[User]) => sqls.eq(itemUser.userId, user.id).and.eq(itemUser.`type`, WantHaveType.Want.toString),
    merge = (item, wantUsers) => item.copy(wantUsers = wantUsers)
  )

  override def tableName: String = "items"

  override def defaultAlias: Alias[Item] = createAlias("i")

  override def extract(rs: WrappedResultSet, n: ResultName[Item]): Item = autoConstruct(rs, n, "users", "wantUsers")

  private def toNamedValues(record: Item): Seq[(Symbol, Any)] = Seq(
    'code     -> record.code,
    'name     -> record.name,
    'url      -> record.url,
    'imageUrl -> record.imageUrl,
    'price    -> record.price,
    'createAt -> record.createAt,
    'updateAt -> record.updateAt
  )

  def create(item: Item)(implicit session: DBSession = AutoSession): Long = createWithAttributes(toNamedValues(item): _*)

  def update(item: Item)(implicit session: DBSession = AutoSession): Int = updateById(item.id.get).withAttributes(toNamedValues(item): _*)

}