using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Linq;
using System.Text.RegularExpressions;
using System.Web;
using System.Xml.Linq;
using Amazon.CloudSearchDomain;
using Amazon.CloudSearchDomain.Model;
using SI4T.Query.CloudSearch.Models;

namespace SI4T.Query.CloudSearch
{
    public class Connection
    {
        /// <summary>
        /// URL of the AWS CloudSearch Search endpoint
        /// </summary>
        public string ServiceUrl { get; set; }
        
        /// <summary>
        /// Number of characters for auto-generated summary data
        /// </summary>
        public int AutoSummarySize { get; set; }
        
        /// <summary>
        /// Default page size
        /// </summary>
        public int DefaultPageSize { get; set; }
        
        /// <summary>
        /// Maximum number of Facets to return
        /// </summary>
        public int MaxNumberOfFacets { get; set; }

        public Connection(string serviceUrl)
        {
            ServiceUrl = serviceUrl;
            AutoSummarySize = 255;
            DefaultPageSize = 10;
            MaxNumberOfFacets = 100;
        }

        /// <summary>
        /// Run a query
        /// </summary>
        /// <param name="parameters">The query parameters</param>
        /// <returns>matching results</returns>
        public CloudSearchResults ExecuteQuery(NameValueCollection parameters)
        {
            CloudSearchResults result = new CloudSearchResults();

            try
            {
                AmazonCloudSearchDomainClient client = GetCloudSearchClient();
                SearchRequest request = BuildSearchRequest(parameters);
                SearchResponse response = client.Search(request);

                result.Items = response.Hits.Hit.Select(hit => CreateSearchResult(hit, null)).ToList();
                result.Facets = (
                        from f in response.Facets
                        select new Facet
                        {
                            Name = f.Key,
                            Buckets = f.Value.Buckets.Select(b => new SI4T.Query.CloudSearch.Models.Bucket(b.Value, b.Count)).ToList()
                        }).ToList();

                result.PageSize = Convert.ToInt32(request.Size);
                result.Total = Convert.ToInt32(response.Hits.Found);
                result.Start = Convert.ToInt32(request.Start);
                result.QueryText = request.Query;
            }
            catch (Exception ex)
            {
                result.HasError = true;
                result.ErrorDetail = ex.Message + " : " + ex.StackTrace;
            }            
            
            return result;
        }

        /// <summary>
        /// Retrieving suggestions using the AWS CloudSearch Suggester. The AWS CloudSearch Suggester
        /// returns results from the entire index and does not suport query filtering.
        /// </summary>
        /// <param name="parameters">Suggest request parameters</param>
        /// <returns>SuggestResults</returns>
        public SuggestResults RetieveSuggestions(NameValueCollection parameters)
        {
            SuggestResults suggestResults = new SuggestResults();

            try
            {
                AmazonCloudSearchDomainClient client = GetCloudSearchClient();

                SuggestRequest request = new SuggestRequest();
                if (parameters["suggestername"] != null)
                {
                    request.Suggester = parameters["suggestername"];
                }

                if (parameters["q"] != null)
                {
                    request.Query = parameters["q"];
                }

                request.Size = parameters["size"] != null ? Convert.ToInt32(parameters["size"]) : this.DefaultPageSize;

                SuggestResponse results = client.Suggest(request);
                suggestResults.Matches = results.Suggest.Suggestions.Select(c => c.Suggestion).ToList();
            }
            catch (Exception ex)
            {
                suggestResults.HasError = true;
                suggestResults.ErrorDetail = ex.Message + " : " + ex.StackTrace;
            }

            return suggestResults;
        }

        /// <summary>
        /// Retrieving suggestions using a custom implementation. This implementation assumes a 'literal'
        /// field type to be used as source for suggestions. This field must have lowercase values.
        /// Optionally an alternate display field can be provided which can be used populate the result.
        /// This implementation returns similar results as the AWS CloudSearch Suggester when populate the
        /// suggest field with the same value field as display field (but in lowercase).
        /// </summary>
        /// <param name="parameters">Suggest request parameters</param>
        /// <param name="suggestFieldName">Name of the suggest field</param>
        /// <param name="displayFieldName">Name of the display field</param>
        /// <returns>SuggestResults</returns>
        public SuggestResults RetieveFilteredSuggestions(NameValueCollection parameters, string suggestFieldName, string displayFieldName = null)
        {
            SuggestResults suggestResults = new SuggestResults();

            try
            {
                AmazonCloudSearchDomainClient client = GetCloudSearchClient();

                SearchRequest request = new SearchRequest();

                if (parameters["q"] != null)
                {
                    request.QueryParser = QueryParser.Structured;
                    request.Query = String.Format("(prefix field={0} '{1}')", suggestFieldName, parameters["q"].ToLower());
                }

                if (displayFieldName == null)
                {
                    request.Facet = String.Format("{{'{0}':{{'sort':'bucket'}}}}", suggestFieldName);
                    request.Return = "_no_fields";
                }
                else
                {
                    request.Return = displayFieldName;
                    request.Sort = String.Format("{0} asc", displayFieldName);
                }

                if (!String.IsNullOrEmpty(parameters["fq"]))
                {
                    string filters = string.Empty;
                    foreach (string filterString in parameters["fq"].Split(','))
                    {
                        if (filterString.Contains(":"))
                        {
                            filters += (String.Format(" {0}:'{1}'", filterString.Split(':')[0], filterString.Split(':')[1]));
                        }
                    }
                    request.FilterQuery = String.Format("(and{0})", filters);
                }

                request.Size = parameters["size"] != null ? Convert.ToInt32(parameters["size"]) : this.DefaultPageSize;

                SearchResponse response = client.Search(request);
                if (displayFieldName == null)
                {
                    if (response.Facets.Count > 0)
                    {
                        suggestResults.Matches = response.Facets[suggestFieldName].Buckets.Select(b => b.Value).ToList();
                    }
                }
                else
                {
                    if (response.Hits.Hit.Count > 0)
                    {
                        suggestResults.Matches = response.Hits.Hit.Select(h => h.Fields[displayFieldName].FirstOrDefault()).ToList();
                    }
                }
            }
            catch (Exception ex)
            {
                suggestResults.HasError = true;
                suggestResults.ErrorDetail = ex.Message + " : " + ex.StackTrace;
            }

            return suggestResults;
        }

        private SearchRequest BuildSearchRequest(NameValueCollection parameters)
        {
            SearchRequest request = new SearchRequest();
            
            //"wt"
            //"fl"
            //"hl.fragsize - highlight

            if (!String.IsNullOrEmpty(parameters["facet"]))
            {
                string facets = string.Join(", ", Array.ConvertAll(parameters["facet"].Split(',').ToArray(), i => String.Format("\"{0}\":{{\"sort\":\"bucket\",\"size\":" + MaxNumberOfFacets +"}}", i.ToString())));
                request.Facet = "{" + facets + "}";
                parameters.Remove("facet");
            }            

            if (!String.IsNullOrEmpty(parameters["fq"]))
            {
                string filters = string.Empty;
                foreach(string filterString in parameters["fq"].Split(','))
                {
                    if(filterString.Contains(":"))
                        filters += (String.Format(" {0}:'{1}'", filterString.Split(':')[0], filterString.Split(':')[1]));
                }
                request.FilterQuery = String.Format("(and{0})",filters);
                parameters.Remove("fq");
            }

            if (!String.IsNullOrEmpty(parameters["rows"]))
            {
                request.Size = Convert.ToInt32(parameters["rows"]);
                parameters.Remove("rows");
            }
            else
            {
                request.Size = this.DefaultPageSize;
            }         
            
            if (!String.IsNullOrEmpty(parameters["sort"]))
            {
                request.Sort = HttpUtility.UrlDecode(parameters["sort"]);
                parameters.Remove("sort");
            }
            else
            {
                //request.Sort = "title desc";
            }

            //hl.contiguous
            int start = 0;
            if (!String.IsNullOrEmpty(parameters["start"]) && Int32.TryParse(parameters["start"], out start))
            {

                request.Start = start;
                parameters.Remove("start");
            }

            if (!String.IsNullOrEmpty(parameters["q.options"]))
            {
                request.QueryOptions = HttpUtility.UrlDecode(parameters["q.options"]);
                parameters.Remove("q.options");
            }

            string searchText = HttpUtility.UrlDecode(parameters["q"]);
            parameters.Remove("q");

            //request.Highlight = searchText;
            if (String.IsNullOrEmpty(searchText))
            {
                request.Query = "*:*";
                request.QueryParser = QueryParser.Lucene;
            }
            else
            {
                if (!String.IsNullOrEmpty(parameters["q.parser"]) && parameters["q.parser"] == "simple")
                {
                    request.Query = searchText;
                    request.QueryParser = QueryParser.Simple;
                }
                else
                {
                    request.Query = "(and (or '" + searchText + "' (prefix '" + searchText + "')) " + string.Join(" ", Array.ConvertAll(parameters.AllKeys, key => string.Format("(and {0}:'{1}')", HttpUtility.UrlEncode(key), HttpUtility.UrlEncode(parameters[key])))) + ")";
                    request.QueryParser = QueryParser.Structured;
                }
            }

            request.Query = HttpUtility.UrlDecode(request.Query);
            
            return request;
        }

        private AmazonCloudSearchDomainClient GetCloudSearchClient()
        {
            //AWSCredentials credentials = new StoredProfileAWSCredentials("development");
            return new AmazonCloudSearchDomainClient(ServiceUrl); 
        }

        private SI4T.Query.Models.SearchResult CreateSearchResult(Hit hit, XElement highlighting)
        {
            SI4T.Query.Models.SearchResult result = new SI4T.Query.Models.SearchResult {Id = hit.Id};

            foreach (KeyValuePair<string, List<string>> field in hit.Fields)
            {
                string type = field.Value.GetType().ToString();
                string fieldname = field.Key;
                
                switch (fieldname)
                {
                    case "publicationid":
                        result.PublicationId = Int32.Parse(field.Value.FirstOrDefault());
                        break;
                    case "title":
                        result.Title = field.Value.FirstOrDefault();
                        break;
                    case "url":
                        result.Url = field.Value.FirstOrDefault();
                        break;
                    case "summary":
                        result.Summary = field.Value.FirstOrDefault();
                        break;
                    default:
                        object data = null;
                        switch (type)
                        {
                            case "arr": //TODO: Make smarter
                                data = field.Value.ToList();
                                break;
                            default:
                                data = field.Value.FirstOrDefault();
                                break;
                        }
                        result.CustomFields.Add(fieldname, data);
                        break;
                }
            }

            if (String.IsNullOrEmpty(result.Summary) && highlighting != null) //TODO: Make this work
            {
                string plainText = Regex.Replace(Regex.Replace(highlighting.Elements("lst").Where(a => a.Attribute("name").Value == result.Id).FirstOrDefault().Value, @"<[^>]*>", String.Empty), @"\s+", " ");
                result.Summary = String.Format("...{0}...", plainText);
            }

            return result;
        }

    }
}
