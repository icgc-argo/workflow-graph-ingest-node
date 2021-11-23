/*
 * Copyright (c) 2021 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc_argo.workflowingestionnode.components;

import static java.lang.String.format;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.icgc_argo.workflowingestionnode.model.GqlAnalysesFetcherResult;
import org.icgc_argo.workflowingestionnode.model.GqlAnalysis;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
public class GqlAnalysesFetcher implements BiFunction<String, String, Optional<GqlAnalysis>> {
  private final String rdpcUrl;
  private final RestTemplate template;

  public Optional<GqlAnalysis> apply(String analysisId, String studyId) {
    val variables = Map.of(ANALYSIS_ID_KEY, analysisId, STUDY_ID_KEY, studyId);
    val body = Map.of("query", GQL_QUERY, "variables", variables);
    val result = template.postForObject(rdpcUrl, body, GqlAnalysesFetcherResult.class);
    return Optional.ofNullable(result)
        .map(GqlAnalysesFetcherResult::getData)
        .map(GqlAnalysesFetcherResult.GqlResultData::getAnalyses)
        .map(GqlAnalysesFetcherResult.GqlResultAnalyses::getContent)
        .map(l -> l.isEmpty() ? null : l.get(0));
  }

  private static final String ANALYSIS_ID_KEY = "analysisId";
  private static final String STUDY_ID_KEY = "studyId";

  private static final String GQL_QUERY =
      format(
          "query ($%s: String, $%s: String) {\n"
              + "  analyses(filter: {analysisId: $%s, studyId: $%s}, page: {size: 1, from: 0}) {\n"
              + "    content {\n"
              + "      analysisId\n"
              + "      studyId\n"
              + "      analysisState\n"
              + "      analysisType\n"
              + "      experiment\n"
              + "      files {\n"
              + "        dataType\n"
              + "      }\n"
              + "      donors {\n"
              + "        donorId\n"
              + "        submitterDonorId\n"
              + "        specimens {\n"
              + "          specimenId\n"
              + "          submitterSpecimenId\n"
              + "          tumourNormalDesignation\n"
              + "          samples {\n"
              + "            sampleId\n"
              + "            submitterSampleId\n"
              + "          }\n"
              + "        }\n"
              + "      }\n"
              + "    }\n"
              + "  }\n"
              + "}\n",
          ANALYSIS_ID_KEY, STUDY_ID_KEY, ANALYSIS_ID_KEY, STUDY_ID_KEY);
}
