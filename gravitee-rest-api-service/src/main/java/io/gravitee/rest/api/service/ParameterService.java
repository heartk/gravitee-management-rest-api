/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service;

import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ParameterService {

    // Parameters for environment

    String findEnv(Key key);

    boolean findEnvAsBoolean(Key key);

    List<String> findAllEnv(Key key);

    Map<String, List<String>> findAllEnv(List<Key> keys);

    <T> List<T> findAllEnv(Key key, Function<String, T> mapper);

    <T> Map<String, List<T>> findAllEnv(List<Key> keys, Function<String, T> mapper);

    <T> List<T> findAllEnv(Key key, Function<String, T> mapper, Predicate<String> filter);

    <T> Map<String, List<T>> findAllEnv(List<Key> keys, Function<String, T> mapper, Predicate<String> filter);

    Parameter saveEnv(Key key, String value);

    Parameter saveEnv(Key key, List<String> value);

    Parameter saveEnv(Key key, Map<String, String> values);

    // Parameters for organization

    String findOrg(Key key);

    boolean findOrgAsBoolean(Key key);

    List<String> findAllOrg(Key key);

    Map<String, List<String>> findAllOrg(List<Key> keys);

    <T> List<T> findAllOrg(Key key, Function<String, T> mapper);

    <T> Map<String, List<T>> findAllOrg(List<Key> keys, Function<String, T> mapper);

    <T> List<T> findAllOrg(Key key, Function<String, T> mapper, Predicate<String> filter);

    <T> Map<String, List<T>> findAllOrg(List<Key> keys, Function<String, T> mapper, Predicate<String> filter);

    Parameter saveOrg(Key key, String value);

    Parameter saveOrg(Key key, List<String> value);

    Parameter saveOrg(Key key, Map<String, String> values);

    // Parameters by reference

    String find(Key key, String referenceId, ParameterReferenceType referenceType);

    boolean findAsBoolean(Key key, String referenceId, ParameterReferenceType referenceType);

    List<String> findAll(Key key, String referenceId, ParameterReferenceType referenceType);

    Map<String, List<String>> findAll(List<Key> keys, String referenceId, ParameterReferenceType referenceType);

    <T> List<T> findAll(Key key, Function<String, T> mapper, String referenceId, ParameterReferenceType referenceType);

    <T> Map<String, List<T>> findAll(List<Key> keys, Function<String, T> mapper, String referenceId, ParameterReferenceType referenceType);

    <T> List<T> findAll(Key key, Function<String, T> mapper, Predicate<String> filter, String referenceId, ParameterReferenceType referenceType);

    <T> Map<String, List<T>> findAll(List<Key> keys, Function<String, T> mapper, Predicate<String> filter, String referenceId, ParameterReferenceType referenceType);

    Parameter save(Key key, String value, String referenceId, ParameterReferenceType referenceType);

    Parameter save(Key key, List<String> value, String referenceId, ParameterReferenceType referenceType);

    Parameter save(Key key, Map<String, String> values, String referenceId, ParameterReferenceType referenceType);
}
