using System;
using System.Collections.Specialized;
using System.Web.Mvc;
using SI4T.Query.Models;
using SI4T.Query.Solr;

namespace YourSite.Controllers
{
    public class SearchController : Controller
    {
        public ActionResult Search(object entity)
        {
            var query = Request.QueryString["q"];
            var results = new SearchResults();
            results.QueryText = query;
            if (!String.IsNullOrEmpty(query))
            {
                //Initialize the connection to the search index
                Connection conn = new Connection("http://localhost:8080/solr/collection1");

                var parameters = new NameValueCollection();
                //Set the query
                parameters["q"] = String.Format("\"{0}\"", query);
                //Add highlighting - enables summary field be auto-generated if empty
                parameters.Add("hl", "true");
                //Add publication id
                parameters.Add("fq", "publicationid:4");
                //Set page size
                parameters.Add("rows", "5"); 
                //Add page number (default is 1)
                String start = Request.QueryString["start"];
                if (!String.IsNullOrEmpty(start))
                {
                    parameters["start"] = start;
                }
                results = conn.ExecuteQuery(parameters);
            }
            return View("SearchResults",results);
        }
    }
}
