package code.accountholder

import code.model._
import code.model.dataAccess.ResourceUser
import code.users.Users
import code.util.Helper.MdcLoggable
import code.util.UUIDString
import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.common.Box


/**
  * the link userId <--> bankId + accountId 
  */
class MapperAccountHolders extends LongKeyedMapper[MapperAccountHolders] with IdPK {

  def getSingleton = MapperAccountHolders

  object user extends MappedLongForeignKey(this, ResourceUser)

  object accountBankPermalink extends UUIDString(this)
  object accountPermalink extends UUIDString(this)

}


object MapperAccountHolders extends MapperAccountHolders with AccountHolders with LongKeyedMetaMapper[MapperAccountHolders] with MdcLoggable  {


  override def dbIndexes = Index(accountBankPermalink, accountPermalink) :: Nil

  def createAccountHolder(userId: Long, bankId: String, accountId: String): Boolean = {
    val holder = MapperAccountHolders.create
      .accountBankPermalink(bankId)
      .accountPermalink(accountId)
      .user(userId)
      .saveMe
    if(holder.saved_?)
      true
    else
      false
  }
  
  
  def getOrCreateAccountHolder(user: User, bankAccountUID :BankIdAccountId): Box[MapperAccountHolders] ={
  
    val mapperAccountHolder = MapperAccountHolders.find(
      By(MapperAccountHolders.user, user.resourceUserId.value),
      By(MapperAccountHolders.accountBankPermalink, bankAccountUID.bankId.value),
      By(MapperAccountHolders.accountPermalink, bankAccountUID.accountId.value)
    )
  
    mapperAccountHolder match {
      case Full(vImpl) => {
        logger.debug(
          s"getOrCreateAccountHolder --> the accountHolder has been existing in server !"
        )
        mapperAccountHolder
      }
      case Empty => {
        val holder: MapperAccountHolders = MapperAccountHolders.create
          .accountBankPermalink(bankAccountUID.bankId.value)
          .accountPermalink(bankAccountUID.accountId.value)
          .user(user.resourceUserId.value)
          .saveMe
        logger.debug(
          s"getOrCreateAccountHolder--> create account holder: $holder"
        )
        Full(holder)
      }
    }
      
  }
  

  def getAccountHolders(bankId: BankId, accountId: AccountId): Set[User] = {
    val results = MapperAccountHolders.findAll(
      By(MapperAccountHolders.accountBankPermalink, bankId.value),
      By(MapperAccountHolders.accountPermalink, accountId.value),
      PreCache(MapperAccountHolders.user)
    )

    results.flatMap { accHolder =>
      ResourceUser.find(By(ResourceUser.id, accHolder.user))
    }.toSet
  }

  def bulkDeleteAllAccountHolders(): Box[Boolean] = {
    Full( MapperAccountHolders.bulkDelete_!!() )
  }

}
