// Build must be run first: cd ../javascript-sdk && npm run build
const { NewsAPI } = require('../dist/index.cjs');

async function main() {
  const client = new NewsAPI('bcsYSbIeGBgCQUW7KmWZQA');

  try {
    console.log('--- Search for "bitcoin" ---');
    const results = await client.search({ q: 'bitcoin', max: 3 });
    console.log(`Total articles: ${results.totalArticles}`);
    for (const article of results.articles) {
      console.log(`  ${article.title}`);
      console.log(`  Source: ${article.source.name}`);
      console.log(`  URL: ${article.url}`);
      console.log();
    }
  } catch (error) {
    console.error(`Error: ${error.message}`);
  }

  try {
    console.log('--- Top Headlines ---');
    const headlines = await client.headlines({ max: 3 });
    console.log(`Total articles: ${headlines.totalArticles}`);
    for (const article of headlines.articles) {
      console.log(`  ${article.title}`);
    }
    console.log();
  } catch (error) {
    console.error(`Error: ${error.message}`);
  }

  try {
    console.log('--- API Usage ---');
    const usage = await client.usage();
    console.log(`Plan: ${usage.plan}`);
    console.log(`Requests today: ${usage.requestsUsed24Hours}/${usage.requestsLimit24Hours}`);
  } catch (error) {
    console.error(`Error: ${error.message}`);
  }
}

main();
