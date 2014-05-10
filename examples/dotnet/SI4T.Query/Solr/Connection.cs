using System;
using System.Configuration;
using System.Collections.Specialized;
using System.IO;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;
using System.Web;
using System.Reflection;
using System.Xml.Linq;
using System.Xml.XPath;
using System.Linq.Expressions;
using System.Linq;
using SI4T.Query.Models;

namespace SI4T.Query.Solr
{
	/// <summary>
	/// Class to do Solr specific searches
	/// </summary>
    public class Connection
    {
        //URL to connect to Solr via HTTP
		public string ServiceUrl { get; set; }
        //Default set of fields to return
		protected string _resultsFields = "id,url,title,summary,publicationid";
		//Number of characters for auto-generated summary data
        public int AutoSummarySize { get; set; }
        //Default page size
		public int DefaultPageSize { get; set; }
		
        public Connection(string serviceUrl)
        {
			ServiceUrl = serviceUrl;
			AutoSummarySize = 255;
			DefaultPageSize = 10;
        }

        /// <summary>
        /// Run a query
        /// </summary>
        /// <param name="parameters">querystring parameters for the query URL</param>
        /// <returns>matching results</returns>
		public SearchResults ExecuteQuery(NameValueCollection parameters)
        {
            var results = new SearchResults();
            try
            {
                string url = BuildQueryUrl(SetParameters(parameters));
                //_log.DebugFormat("Query: {0}", url);
                string data = GetData(url);
				results = ProcessResults(results, data);
                results.PageSize = Int32.Parse(parameters["rows"]);
                //_log.DebugFormat("{0} Results from Query: {1}", results.Total, url);
            }
            catch (Exception ex)
            {
                results.HasError = true;
                results.ErrorDetail = ex.Message + " : " + ex.StackTrace;
                //_log.ErrorFormat("Query resulted in error: {1}", results.ErrorDetail);
            }
            return results;
        }

		protected NameValueCollection SetParameters(NameValueCollection parameters)
		{
			//set some parameter defaults
			parameters["wt"] = "xml";
			if (String.IsNullOrEmpty(parameters["fl"]))
			{
				parameters["fl"] = _resultsFields;
			}
			if (String.IsNullOrEmpty(parameters["hl.fragsize"]))
			{
				parameters["hl.fragsize"] = AutoSummarySize.ToString();
			}
			if (String.IsNullOrEmpty(parameters["rows"]))
			{
				parameters["rows"] = DefaultPageSize.ToString();
			}
			parameters["hl.contiguous"] = "true";
			int start = 0;
			if (!String.IsNullOrEmpty(parameters["start"]) && Int32.TryParse(parameters["start"], out start))
			{
				//Solr uses 0 based indexing
				if (start > 0)
				{
					start = start - 1;
				}
				else
				{
					start = 0;
				}
			}
			parameters["start"] = start.ToString();

			return parameters;
		}

		protected string BuildQueryUrl(NameValueCollection parameters)
		{
			return ServiceUrl + "/select" + BuildQueryString(parameters);
		}

		protected string BuildQueryString(NameValueCollection nvc)
		{
			return "?" + string.Join("&", Array.ConvertAll(nvc.AllKeys, key => string.Format("{0}={1}", HttpUtility.UrlEncode(key), HttpUtility.UrlEncode(nvc[key]))));
		}

        public string GetData(string url)
        {
            string data = String.Empty;
            HttpWebRequest httpWebRequest = (HttpWebRequest)WebRequest.Create(url);
            WebResponse response = httpWebRequest.GetResponse();
            using (Stream responseStream = response.GetResponseStream())
            {
                StreamReader reader = new StreamReader(responseStream, Encoding.UTF8);
                data = reader.ReadToEnd();
            }
            return data;
        }

		protected SearchResults ProcessResults(SearchResults results, string data)
		{
			if (!String.IsNullOrEmpty(data))
			{
                XDocument xmlData = XDocument.Parse(data);
				//All statuses other than zero are error states
                string status = xmlData.Root.Elements("lst").Where(a => a.Attribute("name").Value == "responseHeader").Elements("int").Where(a => a.Attribute("name").Value == "status").FirstOrDefault().Value;
                if (status != "0")
				{
                    results.HasError = true;
                    results.ErrorDetail = "Solr error status: " + status;
                    //_log.ErrorFormat("Query resulted in error: {1}", results.ErrorDetail);
                    return results;
				}
                var highlighting = xmlData.Root.Elements("lst").Where(a => a.Attribute("name").Value == "highlighting").FirstOrDefault();
                XElement result = xmlData.Root.Element("result");
                results.Total = ((int?)result.Attribute("numFound") ?? 0);
				//Solr uses 0-based indexing
                results.Start = ((int?)result.Attribute("start") ?? 0) +1;
				foreach (var item in result.Elements("doc"))
				{
					results.Items.Add(ProcessResult(item,highlighting));
				}
                
			}
			return results;
		}

        private SearchResult ProcessResult(XElement item, XElement highlighting)
        {
            SearchResult sr = new SearchResult();
            foreach(var field in item.Elements())
            {
                string type = field.Name.LocalName;
                string fieldname = field.Attribute("name").Value;
                switch(fieldname)
                {
                    case "id":
                        sr.Id = field.Value;
                        break;
                    case "publicationid":
                        sr.PublicationId = Int32.Parse(field.Value);
                        break;
                    case "title":
                        sr.Title = field.Value;
                        break;
                    case "url":
                        sr.Url = field.Value;
                        break;
                    case "summary":
                        sr.Summary = field.Value;
                        break;
                    default:
                        object data = null;
                        switch (type)
                        {
                            case "arr":
                                data = field.Elements().Select(a => a.Value).ToList();
                                break;
                            default:
                                data = field.Value;
                                break;
                        }
                        sr.CustomFields.Add(fieldname, data);    
                        break;
                }
            }
            if (String.IsNullOrEmpty(sr.Summary) && highlighting != null)
            {
                string plainText = Regex.Replace(Regex.Replace(highlighting.Elements("lst").Where(a => a.Attribute("name").Value == sr.Id).FirstOrDefault().Value, @"<[^>]*>", String.Empty), @"\s+", " ");
                sr.Summary = String.Format("...{0}...", plainText);
            }
            return sr;
        }
    }
}
