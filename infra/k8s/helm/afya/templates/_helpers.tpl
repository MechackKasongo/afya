{{- define "afya.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "afya.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "afya.labels" -}}
helm.sh/chart: {{ include "afya.chart" . }}
app.kubernetes.io/name: {{ include "afya.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "afya.selectorLabels" -}}
app.kubernetes.io/name: {{ include "afya.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "afya.namespace" -}}
{{- .Values.namespace.name }}
{{- end }}

{{- define "afya.image" -}}
{{- $registry := .root.Values.global.imageRegistry -}}
{{- $tag := .root.Values.global.imageTag | default .root.Chart.AppVersion -}}
{{- printf "%s/%s:%s" $registry .image $tag -}}
{{- end }}

{{- define "afya.secretName" -}}
{{- .Values.secrets.name }}
{{- end }}
