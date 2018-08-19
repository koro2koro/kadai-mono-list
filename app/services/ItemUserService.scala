package services

trait ItemUserService {

  def want(userId: Long, itemId: Long): Boolean

  def doNotWant(userId: Long, itemId: Long): Boolean

  def have(userId: Long, itemId: Long): Boolean

  def doNotHave(userId: Long, itemId: Long): Boolean

}