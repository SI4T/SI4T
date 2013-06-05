using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace SI4T.Templating
{
    /// <summary>
    /// Field to encapsulate the characteristics of a field in the search index
    /// </summary>
    public class IndexField
    {
        public string Name { get; set; }
        public bool IsMultiValue { get; set; }
        //TODO possibly add data type, format and other validation bits in future
    }
}
