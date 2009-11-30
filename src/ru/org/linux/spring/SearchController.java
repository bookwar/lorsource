/*
 * Copyright 1998-2009 Linux.org.ru
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

package ru.org.linux.spring;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.SearchViewer;
import ru.org.linux.site.ViewerCacher;

@Controller
public class SearchController {
  @RequestMapping(value="/search.jsp", method= RequestMethod.GET)
  public ModelAndView search(
    @RequestParam(value="q", required=false) String q,
    @RequestParam(value="include", required=false) String includeString,
    @RequestParam(value="date", required=false) String dateString,
    @RequestParam(value="section", required=false) Integer section,
    @RequestParam(value="sort", required=false) Integer sort,
    @RequestParam(value="username", required=false) String username,
    @RequestParam(value="usertopic", required=false) Boolean usertopic
  ) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    boolean initial = q == null;
    params.put("initial", initial);

    if (q==null) {
      q="";
    }

    if (usertopic==null) {
      usertopic = false;
    }

    params.put("usertopic", usertopic);

    params.put("q", q);

    int include = parseInclude(includeString);

    params.put("include", include);

    int date = parseDate(dateString);

    params.put("date", date);

    if (section==null) {
      section = 0;
    }

    params.put("section", section);

    if (sort==null) {
      sort = SearchViewer.SORT_R;
    }

    params.put("sort", sort);

    if (username==null) {
      username = "";
    }

    params.put("username", username);

    if (!initial) {
      SearchViewer sv = new SearchViewer(q);

      sv.setDate(date);
      sv.setInclude(include);
      sv.setSection(section);
      sv.setSort(sort);
      sv.setUser(username);
      sv.setUserTopic(usertopic);

      ViewerCacher cacher = new ViewerCacher();

      params.put("result", cacher.get(sv, false));

      params.put("cached", cacher.isFromCache());

      params.put("time", cacher.getTime());
    }

    return new ModelAndView("search", params);
  }

  public static int parseInclude(String include) {
    if (include==null) {
      return SearchViewer.SEARCH_ALL;
    }

    if ("topics".equals(include)) {
      return SearchViewer.SEARCH_TOPICS;
    }

    return SearchViewer.SEARCH_ALL;
  }

  public static int parseDate(String date) {
    if (date==null) {
      return SearchViewer.SEARCH_YEAR;
    }

    if ("3month".equals(date)) {
      return SearchViewer.SEARCH_3MONTH;
    }

    if ("all".equals(date)) {
      return SearchViewer.SEARCH_ALL;
    }

    return SearchViewer.SEARCH_YEAR;
  }
}
