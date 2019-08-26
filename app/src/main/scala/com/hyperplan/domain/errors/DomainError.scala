/*
 *
 * This file is part of the Hyperplan project.
 * Copyright (C) 2019  Hyperplan
 * Authors: Antoine Sauray
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gn/u.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the Hyperplan software without
 * disclosing the source code of your own applications.
 *
 *
 */

package com.hyperplan.domain.errors

sealed trait DomainError extends Throwable {
  override def getMessage: String
}

case class DomainClassAlreadyExists(domainClassId: String) extends DomainError {
  override def getMessage = s"The domain class $domainClassId already exists"
}

case class DomainClassDataIncorrect(domainClassId: String) extends DomainError {
  override def getMessage = s"The domain class $domainClassId data is incorrect"
}

case class FeaturesClassDoesNotExist(featuresId: String) extends DomainError {
  override def getMessage = s"The features class $featuresId does not exist"
}

case class LabelsClassDoesNotExist(labelsId: String) extends DomainError {
  override def getMessage = s"The labels class $labelsId does not exist"
}
