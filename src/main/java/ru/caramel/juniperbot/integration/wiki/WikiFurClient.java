package ru.caramel.juniperbot.integration.wiki;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.sourceforge.jwbf.core.actions.HttpActionClient;
import net.sourceforge.jwbf.core.contentRep.Article;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.sweble.wikitext.engine.EngineException;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.WtEngineImpl;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.nodes.EngProcessedPage;
import org.sweble.wikitext.parser.nodes.WtNode;
import org.sweble.wikitext.parser.nodes.WtPageName;
import org.sweble.wikitext.parser.nodes.WtRedirect;
import org.sweble.wikitext.parser.nodes.WtText;
import org.sweble.wikitext.parser.parser.LinkTargetException;
import ru.caramel.juniperbot.service.MessageService;

import javax.annotation.PostConstruct;

@Service
public class WikiFurClient {

    private static final String SCRIPT_ENDPOINT = "http://ru.wikifur.com/w/";
    private static final String WIKI_URL = "http://ru.wikifur.com/wiki/";


    private MediaWikiBot client;

    private WikiConfig config = WikiFurConfig.generate();

    private WtEngineImpl engine = new WtEngineImpl(config);

    @Value("${app.version}")
    private String version;

    @Autowired
    private MessageService messageService;

    @PostConstruct
    public void init() {
        HttpActionClient httpActionClient = HttpActionClient.builder()
                .withUrl(SCRIPT_ENDPOINT)
                .withUserAgent("JuniperBot", version, "goldrenard@gmail.com")
                .build();
        client = new MediaWikiBot(httpActionClient);
    }

    public Article getArticle(String search) {
        return client.getArticle(search);
    }

    private EngProcessedPage processedPage(Article article) {
        try {
            PageTitle pageTitle = PageTitle.make(config, article.getTitle());
            PageId pageId = new PageId(pageTitle, Integer.parseInt(article.getRevisionId()));
            return engine.postprocess(pageId, article.getText(), null);
        } catch (LinkTargetException | EngineException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageEmbed renderArticle(Article article) {
        return renderArticle(article, false);
    }

    private MessageEmbed renderArticle(Article article, boolean redirected) {
        if (article == null || StringUtils.isEmpty(article.getRevisionId())) {
            return null;
        }
        EngProcessedPage processedPage = processedPage(article);
        String redirect = lookupRedirect(processedPage);
        if (redirect != null) {
            if (redirected) {
                return null;
            }
            return renderArticle(getArticle(redirect), true);
        }

        EmbedBuilder embedBuilder = messageService.getBaseEmbed();
        embedBuilder.setTitle(article.getTitle(), WIKI_URL + article.getTitle());
        TextConverter converter = new TextConverter(config, embedBuilder);
        return (MessageEmbed) converter.go(processedPage.getPage());
    }

    public String lookupRedirect(WtNode node) {
        if (node instanceof WtRedirect) {
            WtPageName page = ((WtRedirect) node).getTarget();
            if (!page.isEmpty() && page.get(0) instanceof WtText) {
                return ((WtText) page.get(0)).getContent();
            }
        }
        for (WtNode child : node) {
            String result = lookupRedirect(child);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
