using System.Collections.Generic;
using SI4T.Query.Models;

namespace SI4T.Query.CloudSearch.Models
{
    /// <summary>
    /// Extended version of SI4T.Query's <see cref="SearchResults"/> returned by the CloudSearch Provider.
    /// </summary>
    /// <remarks>
    /// The CloudSearch Provider uses this class to provide access to the Facets returned by CloudSearch.
    /// Since regular Solr also supports Facets, this facility should be moved up to <see cref="SearchResults"/>, in which case this class becomes redundant.
    /// </remarks>
    public class CloudSearchResults : SearchResults
    {
        public List<Facet> Facets { get; set; }

        public CloudSearchResults()
        {
            Facets = new List<Facet>();
        }
    }
}
