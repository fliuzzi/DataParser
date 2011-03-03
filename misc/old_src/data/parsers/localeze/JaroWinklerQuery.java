package com.where.data.parsers.localeze;

import java.io.IOException;
import java.util.PriorityQueue;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.FilteredTermEnum;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.SimilarityDelegator;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.Explanation.IDFExplanation;

public class JaroWinklerQuery extends FuzzyQuery
{
    public TopDocs search(IndexSearcher searcher, int numToGet) throws IOException
    {
        //for reasons I can't ascertain, similarity taken from the 
        //searcher is used for idf calculations (although the rest 
        //are handled by the similarity from the query). This seems
        //like a bug, and will perhaps go away
        searcher.setSimilarity(getSimilarity(searcher));
        return searcher.search(this, numToGet);
    }
    
    private static final long serialVersionUID = 3708422653507104477L;

    public JaroWinklerQuery(Term term, float minimumSimilarity)
            throws IllegalArgumentException
    {
        super(term, minimumSimilarity);
    }

    public JaroWinklerQuery(Term term, float minimumSimilarity , int prefixLength)
    throws IllegalArgumentException
    {
        super(term, minimumSimilarity, prefixLength);
    }

    @Override
    protected FilteredTermEnum getEnum(IndexReader reader) throws IOException {
      return new JaroWinklerTermEnum(reader, getTerm(), getMinSimilarity(), getPrefixLength());
    }
    
    public static final class ScoreTerm implements Comparable<ScoreTerm> {
        public Term term;
        public float score;
        public int compareTo(ScoreTerm other) {
          if (this.score == other.score)
            return other.term.compareTo(this.term);
          else
            return Float.compare(this.score, other.score);
        }
      }
    
    @Override
    public Query rewrite(IndexReader reader) throws IOException {
        /*
        if(!termLongEnoughFake) {  // can only match if it's exact
        return new TermQuery(term);
      }
        */
      int maxSize = BooleanQuery.getMaxClauseCount();
      PriorityQueue<ScoreTerm> stQueue = new PriorityQueue<ScoreTerm>();
      FilteredTermEnum enumerator = getEnum(reader);
      try {
        ScoreTerm st = new ScoreTerm();
        do {
          final Term t = enumerator.term();
          if (t == null) break;
          final float score = enumerator.difference();
          // ignore uncompetetive hits
          if (stQueue.size() >= maxSize && score <= stQueue.peek().score)
            continue;
          // add new entry in PQ
          st.term = t;
          st.score = score;
          stQueue.offer(st);
          // possibly drop entries from queue
          st = (stQueue.size() > maxSize) ? stQueue.poll() : new ScoreTerm();
        } while (enumerator.next());
      } finally {
        enumerator.close();
      }
      
      BooleanQuery query = new BooleanQuery(true);
      DisjunctionMaxQuery dmq = new DisjunctionMaxQuery(0);
      for (final ScoreTerm st : stQueue) {
        TermQuery tq = new TermQuery(st.term);      // found a match
        tq.setBoost(st.score); // set the boost
        query.add(tq, BooleanClause.Occur.SHOULD);   // add to query
        dmq.add(query);
      }

      return dmq;
    }
    
    @SuppressWarnings("serial")
    @Override
    public Similarity getSimilarity(Searcher searcher) {
      Similarity result = super.getSimilarity(searcher);
      result = new SimilarityDelegator(result) 
      {
            @Override
            public float coord(int overlap, int maxOverlap) {
              return 1.0f;
            }
            
            @Override
            public float computeNorm(String field, FieldInvertState state) {
              return 1.0F;
            }
            
            /** Implemented as <code>1/sqrt(numTerms)</code>. */
            @Override
            public float lengthNorm(String fieldName, int numTerms) {
              return 1.0F;
            }
            
            /** Implemented as <code>1/sqrt(sumOfSquaredWeights)</code>. */
            @Override
            public float queryNorm(float sumOfSquaredWeights) {
                return 1.0f;
            }

            /** Implemented as <code>sqrt(freq)</code>. */
            @Override
            public float tf(float freq) {
                return freq;
            }
              
            /** Implemented as <code>1 / (distance + 1)</code>. */
            @Override
            public float sloppyFreq(int distance) {
                return (float)distance;
            }
              
            /** Implemented as <code>log(numDocs/(docFreq+1)) + 1</code>. */
            @Override
            public float idf(int docFreq, int numDocs) {
                return 1.0f;
            }
              
            @Override            
            public IDFExplanation idfExplain(final Term term, final Searcher searcher) throws IOException {
                final int df = searcher.docFreq(term);
                final int max = searcher.maxDoc();
                final float idf = 1.0f;
                return new IDFExplanation() {
                    @Override
                    public String explain() {
                      return "idf(docFreq=" + df +
                      ", maxDocs=" + max + ")";
                    }
                    @Override
                    public float getIdf() {
                      return idf;
                    }};
               }

      };
      return result;
    }
}
