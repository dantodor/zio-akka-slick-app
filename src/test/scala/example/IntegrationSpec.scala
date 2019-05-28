package example.application

import com.github.jczuchnowski.interop.slick.DatabaseProvider
import com.github.jczuchnowski.interop.slick.dbio._
import example.domain.PortfolioAssetRepository
import example.domain.PortfolioId
import example.domain.PortfolioAsset
import example.domain.{ Asset, AssetId, AssetRepository }
import org.mockito.Mockito
import org.scalatest._
import scalaz.zio.{ IO, ZIO }
import scalaz.zio.DefaultRuntime
import slick.jdbc.H2Profile.backend._
import example.infrastructure.SlickAssetRepository
import example.infrastructure.SlickPortfolioAssetRepository
import example.Api
import akka.http.scaladsl.testkit.ScalatestRouteTest
import example.infrastructure.tables.{ AssetsTable, PortfolioAssetsTable }
import slick.lifted.TableQuery
import example.JsonSupport

class IntegrationSpec extends FlatSpec with Matchers with DefaultRuntime with ScalatestRouteTest with JsonSupport {
  
  trait TestDatabaseProvider extends DatabaseProvider {
    override val databaseProvider = new DatabaseProvider.Service {
      override val db = ZIO.effectTotal(Database.forConfig("h2mem1"))
    }
  }

  class TestEnv extends SlickAssetRepository with SlickPortfolioAssetRepository with TestDatabaseProvider

  val testEnv = new TestEnv()
  val api = new Api(testEnv)


  val assets = TableQuery[AssetsTable.Assets]
  val portfolioAssets = TableQuery[PortfolioAssetsTable.PortfolioAssets]

  val setup = {
    import slick.jdbc.H2Profile.api._
    DBIO.seq(
      (assets.schema ++ portfolioAssets.schema).create,
      assets += Asset(AssetId(1), "GBPUSD", BigDecimal(100.0)),
      portfolioAssets += PortfolioAsset(PortfolioId(1), AssetId(1), 10)
    )
  }

  val setupIO = ZIO.fromDBIO(setup).provide(testEnv)
  this.unsafeRun(setupIO)
  
  "Assets endpoint" should "return a all assets for GET requests to the /assets path" in {
    Get("/assets") ~> api.route ~> check {
      responseAs[List[Asset]] shouldEqual List(Asset(AssetId(1), "GBPUSD", BigDecimal(100.0)))
    }
  }

}