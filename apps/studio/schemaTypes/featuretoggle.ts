import {defineField, defineType} from 'sanity'

export const featuretoggle = defineType({
  name: 'featuretoggle',
  title: 'Featuretoggle',
  type: 'document',
  fields: [
    defineField({
      name: 'name',
      title: 'Name',
      type: 'string',
      validation: (S) => S.required()
    }),
    defineField({
      name: 'variants',
      title: 'Variants',
      type: 'array',
      of: [{ type: 'variant' }],
    })
  ],
});

export const variant = defineType({
  name: 'variant',
  title: 'Variant',
  type: 'object',
  fields: [
    defineField({
      name: 'name',
      title: 'Name',
      type: 'string',
      validation: (S) => S.required()
    }),
    defineField({
      name: 'constraints',
      title: 'Constraints',
      type: 'array',
      of: [{ type: 'constraint' }],
      validation: (R) => R.min(1)
    }),
    defineField({
      name: 'evaluation',
      title: 'Evaluation',
      type: 'evaluation',
    }),
  ],
  preview: {
    select: {
      name: 'name',
      constraints: 'constraints',
      evaluation: 'evaluation',
    },
    prepare(selection) {
      const {name, constraints, evaluation} = selection

      return {
        title: `${name} (${evaluationPreview(evaluation).title})`,
        subtitle: constraints.map(constraintPreview).map((it: any) => it.title).join(' AND ')
      }
    }
  }
});

export const constraint = defineType({
  name: 'constraint',
  title: 'Constraint',
  type: 'object',
  fields: [
    defineField({
      name: 'type',
      title: 'Type',
      type: 'string',
      options: {
        list: [
          { title: 'Default', value: 'default' },
          { title: 'Comparison', value: 'comparison' },
        ],
        layout: 'radio'
      }
    }),
    defineField({
      name: 'isEnabled',
      title: 'Is Enabled',
      type: 'boolean',
      hidden: ({ parent }) => parent?.type !== 'default'
    }),
    defineField({
      name: 'ctxKey',
      title: 'Context key',
      type: 'string',
      hidden: ({ parent }) => parent?.type !== 'comparison'
    }),
    defineField({
      name: 'operator',
      title: 'Operator',
      type: 'string',
      hidden: ({ parent }) => parent?.type !== 'comparison',
      options: {
        list: [
          { title: 'Equals', value: 'EQUALS' },
          { title: 'Not Equals', value: 'NOT_EQUALS' },
          { title: 'In String', value: 'IN_STRING' },
          { title: 'Not In String', value: 'NOT_IN_STRING' },
          { title: 'In List', value: 'IN_LIST' },
          { title: 'Not In List', value: 'NOT_IN_LIST' },
        ]
      }
    }),
    defineField({
      name: 'value',
      title: 'Value',
      type: 'string',
      hidden: ({ parent }) => parent?.type !== 'comparison'
    }),
  ],
  preview: {
    select: {
      type: 'type',
      isEnabled: 'isEnabled',
      ctxKey: 'ctxKey',
      operator: 'operator',
      value: 'value',
    },
    prepare: constraintPreview,
  }
});

type ConstraintSelection = Record<'type' | 'isEnabled' | 'ctxKey' | 'operator' | 'value', any>;
function constraintPreview(selection: ConstraintSelection) {
  if (selection.type == 'default') {
    return {
      title: `Default: ${selection.isEnabled === true ? 'enabled' : 'disabled'}`
    }
  } else if (selection.type == 'comparison') {
    return {
      title: `$\{${selection.ctxKey}\} ${selection.operator} ${selection.value}`,
      subtitle: 'Comparison'
    }
  } else {
    return {
      title: `Unknown type: ${selection.type}`
    }
  }
}

export const evaluation = defineType({
  name: 'evaluation',
  title: 'Evaluation',
  type: 'object',
  fields: [
    defineField({
      name: 'type',
      title: 'Type',
      type: 'string',
      options: {
        list: [
          { title: 'Fixed', value: 'fixed' },
          { title: 'Gradual', value: 'gradual' },
        ],
        layout: 'radio',
      },
    }),
    defineField({
      name: 'isEnabled',
      title: 'Is Enabled (Fixed)',
      type: 'boolean',
      hidden: ({ parent }) => parent?.type !== 'fixed',
    }),
    defineField({
      name: 'percentage',
      title: 'Percentage (Gradual)',
      type: 'number',
      validation: Rule => Rule.min(0).max(100),
      hidden: ({ parent }) => parent?.type !== 'gradual',
    }),
  ],
})

type EvaluationSelection = Record<'type' | 'isEnabled' | 'percentage', any>
function evaluationPreview(selection: EvaluationSelection) {
  const {type, isEnabled, percentage} = selection;
  if (type === 'fixed') {
    return { title: `Default: ${isEnabled === true ? 'enabled' : 'disabled'}` }
  }
  else if (type === 'gradual') {
    return { title: `Gradual: ${percentage}%` }
  }
  else {
    return { title: `Unknown type: ${type}` }
  }
}