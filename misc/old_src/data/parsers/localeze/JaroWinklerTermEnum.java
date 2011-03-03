package com.where.data.parsers.localeze;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FilteredTermEnum;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.spell.JaroWinklerDistance;

/** Subclass of FilteredTermEnum for enumerating all terms that are similar
 * to the specified filter term.
 *
 * <p>Term enumerations are always ordered by Term.compareTo().  Each term in
 * the enumeration is greater than all that precede it.
 */
public final class JaroWinklerTermEnum extends FilteredTermEnum {
  private float similarity;
  private boolean endEnum = false;
  JaroWinklerDistance dist = new JaroWinklerDistance();

  private Term searchTerm = null;
  private final String field;
  private final String text;
  private final String prefix;

  private final float minimumSimilarity;
  @SuppressWarnings("unused")
private final float scale_factor;

  /**
   * Creates a FuzzyTermEnum with an empty prefix and a minSimilarity of 0.5f.
   * <p>
   * After calling the constructor the enumeration is already pointing to the first 
   * valid term if such a term exists. 
   * 
   * @param reader
   * @param term
   * @throws IOException
   * @see #JaroWinklerTermEnum(IndexReader, Term, float, int)
   */
  public JaroWinklerTermEnum(IndexReader reader, Term term) throws IOException {
    this(reader, term, FuzzyQuery.defaultMinSimilarity, FuzzyQuery.defaultPrefixLength);
  }
    
  /**
   * Creates a FuzzyTermEnum with an empty prefix.
   * <p>
   * After calling the constructor the enumeration is already pointing to the first 
   * valid term if such a term exists. 
   * 
   * @param reader
   * @param term
   * @param minSimilarity
   * @throws IOException
   * @see #JaroWinklerTermEnum(IndexReader, Term, float, int)
   */
  public JaroWinklerTermEnum(IndexReader reader, Term term, float minSimilarity) throws IOException {
    this(reader, term, minSimilarity, FuzzyQuery.defaultPrefixLength);
  }
    
  /**
   * Constructor for enumeration of all terms from specified <code>reader</code> which share a prefix of
   * length <code>prefixLength</code> with <code>term</code> and which have a fuzzy similarity &gt;
   * <code>minSimilarity</code>.
   * <p>
   * After calling the constructor the enumeration is already pointing to the first 
   * valid term if such a term exists. 
   * 
   * @param reader Delivers terms.
   * @param term Pattern term.
   * @param minSimilarity Minimum required similarity for terms from the reader. Default value is 0.5f.
   * @param prefixLength Length of required common prefix. Default value is 0.
   * @throws IOException
   */
  public JaroWinklerTermEnum(IndexReader reader, Term term, final float minSimilarity, final int prefixLength) throws IOException {
    super();
    
    if (minSimilarity >= 1.0f)
      throw new IllegalArgumentException("minimumSimilarity cannot be greater than or equal to 1");
    else if (minSimilarity < 0.0f)
      throw new IllegalArgumentException("minimumSimilarity cannot be less than 0");
    if(prefixLength < 0)
      throw new IllegalArgumentException("prefixLength cannot be less than 0");

    this.minimumSimilarity = minSimilarity;
    //this.scale_factor = 1.0f / (1.0f - minimumSimilarity);
    this.scale_factor = 1.0f;
    this.searchTerm = term;
    this.field = searchTerm.field();
    //The prefix could be longer than the word.
    //It's kind of silly though.  It means we must match the entire word.
    final int fullSearchTermLength = searchTerm.text().length();
    final int realPrefixLength = prefixLength > fullSearchTermLength ? fullSearchTermLength : prefixLength;

    this.text = searchTerm.text().substring(realPrefixLength);
    this.prefix = searchTerm.text().substring(0, realPrefixLength);

    setEnum(reader.terms(new Term(searchTerm.field(), prefix)));
  }

  /**
   * The termCompare method uses Jaro-Winkler distance to 
   * calculate the distance between the given term and the comparing term. 
   */
  @Override
  protected final boolean termCompare(Term term) {
    if (field == term.field() && term.text().startsWith(prefix)) {
        final String target = term.text().substring(prefix.length());
        this.similarity = dist.getDistance(text, target);

        return (similarity > minimumSimilarity);
    }
    endEnum = true;
    return false;
  }
  
  /** {@inheritDoc} */
  @Override
  public final float difference() {
    //return (similarity - minimumSimilarity) * scale_factor;
    return similarity;  
  }
  
  /** {@inheritDoc} */
  @Override
  public final boolean endEnum() {
    return endEnum;
  }
  
  /** {@inheritDoc} */
  @Override
  public void close() throws IOException {
    searchTerm = null;
    super.close();  //call super.close() and let the garbage collector do its work.
  }
  
}
