import zio.test.ZIOSpecDefault
import zio.test.Spec
import zio.test.TestEnvironment
import zio.ZIO
import zio.stream.ZStream
import zio.test.*
import zio.ZLayer
import com.redis.testcontainers.RedisContainer
import zio.testcontainers.*
import org.testcontainers.utility.DockerImageName
import zio.redis.RedisConfig
import zio.redis.{Redis, AsyncRedis}
import zio.redis.CodecSupplier
import redis.clients.jedis.Jedis

object MainSpec extends ZIOSpecDefault:

  val ChunkSize = 4096

  val redisContainerLayer: ZLayer[Any, Nothing, RedisConfig] =
    ZLayer.scoped:
      RedisContainer(DockerImageName.parse("bitnami/redis:7.2"))
        .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
        .toZIO
        .map(c => RedisConfig(c.getHost(), c.getFirstMappedPort(), ChunkSize))

  val jedisLayer = ZLayer.scoped:
    ZIO.serviceWithZIO[RedisConfig]: cfg =>
      ZIO.fromAutoCloseable(ZIO.attemptBlocking(Jedis(cfg.host, cfg.port)))

  val input = ZStream.range(0, 1024 * 1024, ChunkSize)

  override def spec: Spec[TestEnvironment, Any] =
    suite("bla")(
      test("zio-redis"):
        ZIO.serviceWithZIO[AsyncRedis]: aredis =>
          input
            .mapChunksZIO(_.mapZIO(i => aredis.set(i, i)))
            .bufferChunks(1)
            .mapChunksZIO(_.mapZIO(identity))
            .runDrain
            .as(assertTrue(true))
      ,
      test("jedis"):
        ZIO.serviceWithZIO[Jedis]: jedis =>
          input
            .mapChunksZIO: chunk =>
              ZIO.scoped:
                ZIO
                  .fromAutoCloseable(ZIO.succeed(jedis.pipelined()))
                  .flatMap: pipeline =>
                    chunk.mapZIO(i => ZIO.succeed(pipeline.set(i.toString, i.toString)))
            .runDrain
            .as(assertTrue(true))
    )
      .provideSomeShared[TestEnvironment](
        redisContainerLayer,
        jedisLayer,
        Redis.singleNode,
        ZLayer.succeed(CodecSupplier.utf8)
      ) @@ TestAspect.sequential @@ TestAspect.timed
