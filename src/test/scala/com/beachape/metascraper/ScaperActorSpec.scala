package com.beachape.metascraper

import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.{TestActorRef, TestKit, ImplicitSender}
import akka.actor.ActorSystem
import scala.io.Source
import org.jsoup.Jsoup

class ScraperActorSpec extends TestKit(ActorSystem("testSystem"))
  with FunSpec
  with ShouldMatchers
  with BeforeAndAfter
  with ImplicitSender {

  val scraperActorRef = TestActorRef(new ScraperActor)
  val scraperActor = scraperActorRef.underlyingActor

  lazy val withoutOgTagsSource = Source.fromURL(getClass.getResource("/withoutOgTags.html"))
  lazy val withoutOgTagsDoc = Jsoup.parse(withoutOgTagsSource.mkString)

  lazy val withOgTagsSource = Source.fromURL(getClass.getResource("/withOgTags.html"))
  lazy val withOgTagsDoc = Jsoup.parse(withOgTagsSource.mkString)

  lazy val withoutAnyTagsSource = Source.fromURL(getClass.getResource("/withoutAnyTags.html"))
  lazy val withoutAnyTagsDoc = Jsoup.parse(withoutAnyTagsSource.mkString)

  describe("#extractUrl") {

    describe("for a page without og:url") {

      it("should return the passed in accessedUrl") {
        scraperActor.extractUrl(withoutOgTagsDoc, "test") should be("test")
      }

    }

    describe("for a page with a meta og:url tag") {

      it("should return the content of the meta og:url tag") {
        scraperActor.extractUrl(withOgTagsDoc, "test") should be("https://ogtagsthingy.com/page.html")
      }

    }

  }

  describe("#extractTitle") {

    describe("for a page without og:title but with a title tag") {

      it("should return the contents of the <title> tag") {
        scraperActor.extractTitle(withoutOgTagsDoc) should be("Without Open Graph tags")
      }

    }

    describe("for a page with og:title") {

      it("should return the contents of the og:title tag") {
        scraperActor.extractTitle(withOgTagsDoc) should be("Title in Open Graph Tag")
      }

    }
  }

  describe("for a page without og:title and title tags") {

    it("should an empty string") {
      scraperActor.extractTitle(withoutAnyTagsDoc) should be("")
    }

  }

  describe("#extractDescription") {

    describe("for a page without og:description but with a meta description tag") {

      it("should return the contents of the <meta name='description' .. > tag") {
        scraperActor.extractDescription(withoutOgTagsDoc) should be("A Description in heeere")
      }

    }

    describe("for a page with a og:description tag") {

      it("should return the contents of the og:description tag") {
        scraperActor.extractDescription(withOgTagsDoc) should be("Description inside og:description tag")
      }

    }

    describe("for a page without og:description and meta description tags") {

      it("should return an empty string") {
        scraperActor.extractDescription(withoutAnyTagsDoc) should be("")
      }

    }
  }

  describe("#extractImages") {

    describe("for a page without og:image tags") {

      it("should return the src contents of the first 5 image tags") {
        scraperActor.extractImages(withoutOgTagsDoc) should be(
          Seq(
            "http://lolol.com/thing1.gif",
            "http://lolol.com/thing2.jpg",
            "http://lolol.com/thing3.jpg",
            "http://lolol.com/thing4.png",
            "http://lolol.com/thing5.jpg"))
      }

    }

    describe("for a page with og:image tags") {

      it("should return the src contents of 5 image sources, prioritising og:image tag contents") {
        scraperActor.extractImages(withOgTagsDoc) should be(
          Seq(
            "http://lala.com/theMainImage.png",
            "http://lolol.com/thing1.gif",
            "http://lolol.com/thing2.jpg",
            "http://lolol.com/thing3.jpg",
            "http://lolol.com/thing4.png"))
      }

    }

    describe("for a page with neither og:image nor img tags") {

      it("should return an empty sequence") {
        scraperActor.extractImages(withoutAnyTagsDoc) should be('empty)
      }

    }
  }

  describe("#extractMainImage") {

    describe("for a page without og:image tags") {

      it("should return the src contents of the first image tag") {
        scraperActor.extractMainImage(withoutOgTagsDoc) should equal("http://lolol.com/thing1.gif")
      }

    }

    describe("for a page with og:image tags") {

      it("should return the src contents of the first og:image tag") {
        scraperActor.extractMainImage(withOgTagsDoc) should equal("http://lala.com/theMainImage.png")
      }

    }

    describe("for a page with neither og:image nor img tags") {

      it("should return an empty string") {
        scraperActor.extractMainImage(withoutAnyTagsDoc) should be("")
      }

    }

  }

  describe("#extractScrapedData") {

    describe("for a page without OG tags") {

      val scrappedData = scraperActor.extractScrapedData(withoutOgTagsDoc, "test")

      it("should return a ScrapedData message with proper attributes") {
        scrappedData.title should be("Without Open Graph tags")
        scrappedData.description should be("A Description in heeere")
        scrappedData.url should be("test")
        scrappedData.mainImageUrl should be("http://lolol.com/thing1.gif")
        scrappedData.imageUrls should be(
          Seq(
            "http://lolol.com/thing1.gif",
            "http://lolol.com/thing2.jpg",
            "http://lolol.com/thing3.jpg",
            "http://lolol.com/thing4.png",
            "http://lolol.com/thing5.jpg"))
      }

    }

    describe("for a page with og tags") {

      val scrappedData = scraperActor.extractScrapedData(withOgTagsDoc, "test")

      it("should return a ScrapedData message with proper attributes") {
        scrappedData.title should be("Title in Open Graph Tag")
        scrappedData.description should be("Description inside og:description tag")
        scrappedData.url should be("https://ogtagsthingy.com/page.html")
        scrappedData.mainImageUrl should be("http://lala.com/theMainImage.png")
        scrappedData.imageUrls should be(
          Seq(
            "http://lala.com/theMainImage.png",
            "http://lolol.com/thing1.gif",
            "http://lolol.com/thing2.jpg",
            "http://lolol.com/thing3.jpg",
            "http://lolol.com/thing4.png"))
      }

    }

    describe("for a page with no required tags") {

      val scrappedData = scraperActor.extractScrapedData(withoutAnyTagsDoc, "test")

      it("should return a ScrapedData message with proper attributes") {
        scrappedData.title should be("")
        scrappedData.description should be("")
        scrappedData.url should be("test")
        scrappedData.mainImageUrl should be("")
        scrappedData.imageUrls should be('empty)
      }

    }

  }

}
