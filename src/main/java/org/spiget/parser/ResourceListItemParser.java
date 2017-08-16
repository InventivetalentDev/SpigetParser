package org.spiget.parser;

import lombok.extern.log4j.Log4j2;
import org.jsoup.nodes.Element;
import org.spiget.data.author.ListedAuthor;
import org.spiget.data.category.ListedCategory;
import org.spiget.data.resource.ListedResource;
import org.spiget.data.resource.Rating;
import org.spiget.data.resource.SpigetIcon;
import org.spiget.data.resource.version.ListedResourceVersion;

import static org.spiget.parser.ParserUtil.*;

/**
 * Parser for items of the resources list
 */
@Log4j2
public class ResourceListItemParser {

	static boolean debug = false;

	/**
	 * Parses a resource item.
	 *
	 * @param resourceItem &lt;li class="resourceListItem" id="resource-1234"&gt;
	 * @return the parsed item
	 */
	public ListedResource parse(Element resourceItem) {
		if (debug) {
			log.debug(resourceItem);
		}

		ListedResource listedResource = new ListedResource(Integer.parseInt(resourceItem.id().replace("resource-", "")));// <li class="resourceListItem visible " id="resource-12345">

		{
			Element resourceTitle = resourceItem.select("h3.title").first();
			Element resourceLink = resourceTitle.getElementsByTag("a").first();// <a href="resources/example-resource.12345/">Example Resource</a>
			Element resourceVersion = resourceTitle.select("span.version").first();// <span class="version">1.2.3</span>

			listedResource.setName(resourceLink.text());
			listedResource.setVersion(new ListedResourceVersion(0, resourceVersion.text(), 0L));// set the date to 0 here, will be updated when parsing the update date
		}

		{
			Element resourceDetails = resourceItem.select("div.resourceDetails").first();
			{
				Element resourceAuthor = resourceDetails.select("a.username").first();// <a href="members/example.1234/" class="username" dir="auto">Example</a>
				listedResource.setAuthor(new ListedAuthor(Integer.parseInt(extractIdFromUrl(resourceAuthor.attr("href"), DOT_URL_ID)), resourceAuthor.text(), null));
			}
			{
				Element resourceReleaseDate = abbrOrSpan(resourceDetails, ".DateTime");// <span class="DateTime" title="May 27, 2016 at 5:20 PM">May 27, 2016</span>
				listedResource.setReleaseDate(parseTimeOrTitle(resourceReleaseDate));
			}
			{
				Element resourceCategory = null;
				for (Element element : resourceDetails.getAllElements()) {
					if (element.hasAttr("href") && element.attr("href").startsWith("resources/categories/")) {
						resourceCategory = element;
						break;
					}
				}
				if (resourceCategory != null) {// <a href="resources/categories/misc.16/">Misc</a>
					listedResource.setCategory(new ListedCategory(Integer.parseInt(extractIdFromUrl(resourceCategory.attr("href"), DOT_URL_ID)), resourceCategory.text()));
				}
			}
		}

		{// Load the icons later, so we can modify the author object
			Element resourceImage = resourceItem.select("div.resourceImage").first();

			{
				Element resourceIcon = resourceImage.select("a.resourceIcon").first();
				SpigetIcon icon = new IconParser().parse(resourceIcon);
				listedResource.setIcon(icon);
			}
			{
				Element resourceAvatar = resourceImage.select("a.avatar").first();
				SpigetIcon avatar = new IconParser().parse(resourceAvatar);
				listedResource.getAuthor().setIcon(avatar);
			}
		}

		{
			Element resourceTagLine = resourceItem.select("div.tagLine").first();
			listedResource.setTag(resourceTagLine.text());
		}

		{
			Element resourceStats = resourceItem.select("div.resourceStats").first();
			{
				Element resourceRatingContainer = resourceStats.select("div.rating").first();
				{
					Rating rating = new RatingParser().parse(resourceRatingContainer);
					listedResource.setRating(rating);
				}
			}
			{
				Element resourceDownloads = resourceStats.select("dl.resourceDownloads").first();
				Element resourceDownloadNumber = resourceDownloads.select("dd").first();// <dd>51</dd>
				listedResource.setDownloads(Integer.parseInt(stringToInt(resourceDownloadNumber.text())));
			}
			{
				Element resourceUpdated = abbrOrSpan(resourceStats, ".DateTime");// <abbr class="DateTime" data-time="1466598083" data-diff="12" data-datestring="Jun 22, 2016" data-timestring="2:21 PM" title="Jun 22, 2016 at 2:21 PM">3 minutes ago</abbr>
				long updateDate = parseTimeOrTitle(resourceUpdated);
				listedResource.setUpdateDate(updateDate);
				listedResource.getVersion().setReleaseDate(updateDate);// Update the date for the previously set version
			}
		}

		{
			Element cost = resourceItem.select("span.cost").first();
			if (cost != null) {// Premium resource
				listedResource.setPremium(true);
				String[] costSplit = cost.text().split(" ");
				if (costSplit.length == 2) {
					try {
						listedResource.setPrice(Double.parseDouble(costSplit[0]));
					} catch (NumberFormatException e) {
						listedResource.setPrice(0);
					}
					listedResource.setCurrency(costSplit[1]);
				}
			}
		}

		return listedResource;
	}

}
