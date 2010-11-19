/*
 * Copyright 1998-2010 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.site;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.SimpleOrderedMap;
import java.net.MalformedURLException;
import org.apache.solr.client.solrj.SolrServerException;

import com.google.common.collect.ImmutableList;

public class SearchViewer {
  public static final int SEARCH_TOPICS = 1;
  public static final int SEARCH_ALL = 0;

  public static final int SEARCH_3MONTH = 1;
  public static final int SEARCH_YEAR = 2;

  public static final int SORT_R = 1;
  public static final int SORT_DATE = 2;

  private final String query;
  private int include = SEARCH_ALL;
  private int date = SEARCH_ALL;
  private int section = 0;
  private int sort = SORT_R;

  private String username = "";
  private boolean userTopic = false;

  public SearchViewer(String query) {
    this.query = query;
  }

  public List<SearchItem> show(Connection db) throws SQLException, UserErrorException,MalformedURLException,SolrServerException {
    QueryResponse response;
    SolrServer solr = new CommonsHttpSolrServer("http://stress.vyborg.ru/solr");
    ModifiableSolrParams params = new ModifiableSolrParams();
    List<SearchItem> items = new ArrayList<SearchItem>();
    // set search query params
    params.set("q", query);
    params.set("q.op", "AND");
    params.set("rows", 100);
    if(include != SEARCH_ALL){
      params.set("fq","is_comment:false");      
    }
    if(date == SEARCH_3MONTH){
      params.set("fq","postdate:[NOW-3MONTH TO NOW]");
    }else if (date == SEARCH_YEAR){
      params.set("fq","postdate:[NOW-1YEAR TO NOW]");
    }
    if (section != 0 ){
      params.set("fq","section_id:"+section);
    }
    if (username.length()>0) {
      try {
        User user = User.getUser(db, username);
        if (userTopic) {
          params.set("fq","user_id:"+user.getId());

        }
      } catch (UserNotFoundException ex) {
        throw new UserErrorException("User not found: "+username);
      }
    }

    // send search query to solr
    response = solr.query(params);
    SolrDocumentList list = response.getResults();
    for (SolrDocument doc : list) {
        items.add(new SearchItem(db, doc));
    }
    return ImmutableList.copyOf(items);

    /*
    StringBuilder select = new StringBuilder(""+
        "SELECT " +
        "msgs.id, msgs.title, msgs.postdate, topic, msgs.userid, rank(idxFTI, q) as rank, message, bbcode");

    if (include==SEARCH_ALL) {
      select.append(" FROM msgs_and_cmts as msgs, msgbase, plainto_tsquery(?) as q");
    } else {
      select.append(" FROM msgs, msgbase, plainto_tsquery(?) as q");
    }

    if (section!=0 || (userTopic && username.length()>0)) {
      select.append(", topics");
    }

    if (section!=0) {
      select.append(", groups");
    }

    select.append(" WHERE msgs.id = msgbase.id AND not msgs.deleted AND idxFTI @@ q");

    if (date==SEARCH_3MONTH) {
      select.append(" AND msgbase_postdate>CURRENT_TIMESTAMP-'3 month'::interval");
    } else if (date == SEARCH_YEAR) {
      select.append(" AND msgbase_postdate>CURRENT_TIMESTAMP-'1 year'::interval");
    }

    if (section!=0) {
      select.append(" AND section=").append(section);
      select.append(" AND topics.id = topic AND groups.id = topics.groupid");
    }

    if (username.length()>0) {
      try {
        User user = User.getUser(db, username);

        if (userTopic) {
          select.append(" AND topics.userid=").append(user.getId());
          select.append(" AND topics.id = topic");
        } else {
          select.append(" AND msgs.userid=").append(user.getId());
        }
      } catch (UserNotFoundException ex) {
        throw new UserErrorException("User not found: "+username);
      }
    }

    if (sort==SORT_DATE) {
      select.append(" ORDER BY postdate DESC");
    } else {
      select.append(" ORDER BY rank DESC");
    }

    select.append(" LIMIT 100");

    PreparedStatement pst = null;

    try {
      pst = db.prepareStatement(select.toString());

      pst.setString(1, query);

      ResultSet rs = pst.executeQuery();

      List<SearchItem> items = new ArrayList<SearchItem>();

      while (rs.next()) {
        items.add(new SearchItem(db, rs));
      }

      return ImmutableList.copyOf(items);
    } finally {
      if (pst!=null) {
        pst.close();
      }
    }
    */

  }

  public String getVariantID() {
    try {
      return "search?q="+ URLEncoder.encode(query, "koi8-r")+"&include="+include+"&date="+date+"&section="+section+"&sort="+sort+"&username="+URLEncoder.encode(username);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public void setInclude(int include) {
    this.include = include;
  }

  public void setDate(int date) {
    this.date = date;
  }

  public void setSection(int section) {
    this.section = section;
  }

  public void setSort(int sort) {
    this.sort = sort;
  }

  public void setUser(String username) {
    this.username = username;
  }

  public void setUserTopic(boolean userTopic) {
    this.userTopic = userTopic;
  }
}
